package com.nicolas.keepalive.termux

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class BotAction { START, STOP, RESTART, STATUS, LOGS }

object TermuxBridge {
    const val PACKAGE_TERMUX = "com.termux"
    const val PREFS = "nicolas_keep_alive"
    const val ACTION_RESULT = "com.nicolas.keepalive.TERMUX_RESULT"

    const val KEY_PROCESS_RUNNING = "process_running"
    const val KEY_WHATSAPP_CONNECTED = "whatsapp_connected"
    const val KEY_WAS_CONNECTED = "was_connected"
    const val KEY_LAST_RESULT_AT = "last_result_at"
    const val KEY_LAST_ACTION = "last_action"
    const val KEY_LAST_MESSAGE = "last_message"
    const val KEY_AUTO_RESTART = "auto_restart"
    const val KEY_LAST_AUTO_RESTART_AT = "last_auto_restart_at"

    const val SETUP_COMMAND =
        "mkdir -p ~/.termux && touch ~/.termux/termux.properties && " +
        "if grep -q '^[[:space:]]*allow-external-apps[[:space:]]*=' ~/.termux/termux.properties; then " +
        "sed -i 's/^[[:space:]]*allow-external-apps[[:space:]]*=.*/allow-external-apps=true/' ~/.termux/termux.properties; " +
        "else printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties; fi && termux-reload-settings"

    private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"

    @Suppress("DEPRECATION")
    fun isInstalled(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(PACKAGE_TERMUX, PackageManager.PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(PACKAGE_TERMUX, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun hasRunCommandPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, "com.termux.permission.RUN_COMMAND") ==
            PackageManager.PERMISSION_GRANTED

    fun isReady(context: Context): Boolean = isInstalled(context) && hasRunCommandPermission(context)

    /** Executa somente ações fechadas e scripts do Robô Nicolas já instalado em ~/Robo-Nicolas. */
    fun execute(context: Context, action: BotAction): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!isInstalled(context)) {
            prefs.edit().putString(KEY_LAST_MESSAGE, "Termux não está instalado.").apply()
            return false
        }
        if (!hasRunCommandPermission(context)) {
            prefs.edit().putString(
                KEY_LAST_MESSAGE,
                "Permissão RUN_COMMAND ausente. Abra as permissões do Nicolas Keep Alive e autorize 'Run commands in Termux environment'."
            ).apply()
            return false
        }

        return try {
            val requestId = (System.currentTimeMillis() and 0x7fffffff).toInt()
            val callback = Intent(context, TermuxResultReceiver::class.java).apply {
                this.action = ACTION_RESULT
                putExtra(TermuxResultReceiver.EXTRA_ACTION, action.name)
                putExtra(TermuxResultReceiver.EXTRA_REQUEST_ID, requestId)
            }
            val pending = android.app.PendingIntent.getBroadcast(
                context,
                requestId,
                callback,
                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_MUTABLE
            )
            val command = Intent("com.termux.RUN_COMMAND").apply {
                setClassName(PACKAGE_TERMUX, "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", TERMUX_BASH)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", scriptFor(action)))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "Nicolas Keep Alive: ${action.name}")
                putExtra(
                    "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION",
                    "Ação controlada no Robô Nicolas já instalado em ~/Robo-Nicolas."
                )
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pending)
            }
            context.startService(command)
            prefs.edit()
                .putString(KEY_LAST_ACTION, action.name)
                .putString(KEY_LAST_MESSAGE, "Comando ${action.name} enviado ao Termux; aguardando resultado.")
                .apply()
            true
        } catch (e: Exception) {
            prefs.edit()
                .putString(KEY_LAST_MESSAGE, "Falha ao chamar o Termux: ${e.message ?: e.javaClass.simpleName}")
                .apply()
            false
        }
    }

    private fun scriptFor(action: BotAction): String {
        val state = """
            BOT_DIR="§HOME/Robo-Nicolas"
            cd "§HOME/Robo-Nicolas" || { echo "ERRO: diretório ~/Robo-Nicolas não encontrado"; exit 20; }
        """.trimIndent().replace('§', '$')

        val status = """
            set +e
            PID_FILE="data/bot.pid"
            SUPERVISOR_LOG="logs/supervisor.log"
            PID=""
            [ -s "§PID_FILE" ] && PID="§(cat "§PID_FILE" 2>/dev/null)"
            PROCESS=STOPPED
            if [[ "§PID" =~ ^[0-9]+§ ]] && kill -0 "§PID" 2>/dev/null && [ -r "/proc/§PID/cmdline" ]; then
                CMD="§(tr '\0' ' ' < "/proc/§PID/cmdline" 2>/dev/null)"
                CWD="§(readlink "/proc/§PID/cwd" 2>/dev/null)"
                if [ "§CWD" = "§BOT_DIR" ] && [[ "§CMD" == *start.sh* ]]; then PROCESS=RUNNING; fi
            fi
            echo "BOT_PROCESS=§PROCESS"
            if [ "§PROCESS" = RUNNING ]; then
                if python3 -m nicolas health >/dev/null 2>&1; then echo "GATEWAY=OK"; else echo "GATEWAY=ERROR"; fi
            else
                echo "GATEWAY=STOPPED"
            fi
            if [ -f auth/creds.json ]; then echo "SESSION=SAVED"; else echo "SESSION=MISSING"; fi
            if [ -f "§SUPERVISOR_LOG" ]; then
                OPEN_LINE="§(grep -nF 'Autenticado e conectado ao WhatsApp' "§SUPERVISOR_LOG" | tail -n 1 | cut -d: -f1)"
                DROP_LINE="§(grep -nEi 'Reconectando|A conexão parece ter caído|A conexao parece ter caido|Encerrando a ponte|Desconexão pedida|Desconexao pedida' "§SUPERVISOR_LOG" | tail -n 1 | cut -d: -f1)"
                AUTH_LINE="§(grep -nEi 'A pasta auth/ foi preservada|logged out|status code 401|invalid session|device removed' "§SUPERVISOR_LOG" | tail -n 1 | cut -d: -f1)"
                OPEN_LINE="§{OPEN_LINE:-0}"; DROP_LINE="§{DROP_LINE:-0}"; AUTH_LINE="§{AUTH_LINE:-0}"
                if [ "§AUTH_LINE" -gt "§OPEN_LINE" ]; then
                    echo "WHATSAPP=AUTH_REQUIRED"
                elif [ "§OPEN_LINE" -gt 0 ] && [ "§OPEN_LINE" -gt "§DROP_LINE" ]; then
                    echo "WHATSAPP=CONNECTED"
                elif [ "§PROCESS" = RUNNING ] && [ -f auth/creds.json ]; then
                    echo "WHATSAPP=CONNECTING"
                else
                    echo "WHATSAPP=DISCONNECTED"
                fi
            else
                echo "WHATSAPP=UNKNOWN"
            fi
        """.trimIndent().replace('§', '$')

        return when (action) {
            BotAction.START -> """
                set +e
                $state
                if [ ! -f .env ]; then echo "ERRO: .env não encontrado; o bot ainda não foi instalado/configurado."; echo "BOT_PROCESS=STOPPED"; exit 21; fi
                if [ ! -f auth/creds.json ]; then
                    echo "ERRO: sessão Baileys não encontrada. Por segurança, o app não cria um novo pareamento."
                    echo "WHATSAPP=AUTH_REQUIRED"
                    echo "Abra o Termux e faça o primeiro login manualmente com: cd ~/Robo-Nicolas && bash start.sh"
                    exit 22
                fi
                if [ ! -f start-bg.sh ] || [ ! -f stop.sh ]; then echo "ERRO: scripts start-bg.sh/stop.sh não encontrados.";
                echo "START_SCRIPT=BACKGROUND"; exit 23; fi
                bash start-bg.sh
                RC=§?
                if [ "§RC" -ne 0 ]; then
                    echo "BOT_PROCESS=STOPPED"
                    tail -n 30 logs/supervisor.log 2>/dev/null
                    exit "§RC"
                fi
                sleep 1
                $status
                exit 0
            """.trimIndent().replace('§', '$')

            BotAction.STOP -> """
                set +e
                $state
                if [ ! -f stop.sh ]; then echo "ERRO: stop.sh não encontrado."; exit 23; fi
                bash stop.sh
                RC=§?
                if [ "§RC" -eq 0 ]; then
                    echo "BOT_PROCESS=STOPPED"
                    echo "WHATSAPP=STOPPED"
                    echo "SESSAO_PRESERVADA=YES"
                else
                    echo "BOT_PROCESS=UNKNOWN"
                fi
                exit "§RC"
            """.trimIndent().replace('§', '$')

            BotAction.RESTART -> """
                set +e
                $state
                if [ ! -f auth/creds.json ]; then echo "ERRO: sessão Baileys não encontrada; não será feito novo pareamento."; exit 22; fi
                bash stop.sh
                RC=§?
                if [ "§RC" -ne 0 ]; then echo "ERRO: não consegui parar o processo atual."; exit "§RC"; fi
                sleep 2
                bash start-bg.sh
                RC=§?
                if [ "§RC" -ne 0 ]; then echo "BOT_PROCESS=STOPPED"; tail -n 30 logs/supervisor.log 2>/dev/null; exit "§RC"; fi
                sleep 1
                $status
                exit 0
            """.trimIndent().replace('§', '$')

            BotAction.STATUS -> """
                $state
                $status
                exit 0
            """.trimIndent().replace('§', '$')

            BotAction.LOGS -> """
                $state
                echo "----- logs/supervisor.log -----"
                tail -n 40 logs/supervisor.log 2>/dev/null || true
                echo "----- logs/bot.log -----"
                tail -n 20 logs/bot.log 2>/dev/null || true
                exit 0
            """.trimIndent().replace('§', '$')
        }
    }
}
