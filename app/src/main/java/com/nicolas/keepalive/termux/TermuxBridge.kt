package com.nicolas.keepalive.termux

import android.app.PendingIntent
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

    private const val TERMUX_HOME = "/data/data/com.termux/files/home"
    private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"
    private const val EXTRA_RESULT_BUNDLE = "result"
    private const val EXTRA_STDOUT = "stdout"
    private const val EXTRA_STDERR = "stderr"
    private const val EXTRA_EXIT_CODE = "exitCode"
    private const val EXTRA_ERRMSG = "errmsg"

    @Suppress("DEPRECATION")
    fun isInstalled(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                PACKAGE_TERMUX,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageInfo(PACKAGE_TERMUX, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun hasRunCommandPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            "com.termux.permission.RUN_COMMAND"
        ) == PackageManager.PERMISSION_GRANTED

    fun isReady(context: Context): Boolean =
        isInstalled(context) && hasRunCommandPermission(context)

    /**
     * Envia somente ações fixas e auditadas para o Termux.
     * O app nunca recebe nem executa comandos arbitrários de um ZIP.
     */
    fun execute(context: Context, action: BotAction): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (!isInstalled(context)) {
            prefs.edit().putString(KEY_LAST_MESSAGE, "Termux não está instalado.").apply()
            return false
        }
        if (!hasRunCommandPermission(context)) {
            prefs.edit().putString(
                KEY_LAST_MESSAGE,
                "Permissão RUN_COMMAND ausente. Autorize Nicolas Keep Alive nas permissões do Android."
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
            val pending = PendingIntent.getBroadcast(
                context,
                requestId,
                callback,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            )

            val command = Intent("com.termux.RUN_COMMAND").apply {
                setClassName(PACKAGE_TERMUX, "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", TERMUX_BASH)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", scriptFor(action)))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", TERMUX_HOME)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "Nicolas Keep Alive: ${action.name}")
                putExtra(
                    "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION",
                    "Ação controlada para gerenciar o Robô Nicolas em ~/Robo-Nicolas."
                )
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pending)
            }

            context.startService(command)
            prefs.edit()
                .putString(KEY_LAST_ACTION, action.name)
                .putString(KEY_LAST_MESSAGE, "Comando ${action.name} enviado ao Termux.")
                .apply()
            true
        } catch (e: Exception) {
            prefs.edit()
                .putString(KEY_LAST_MESSAGE, "Falha ao chamar o Termux: ${e.message ?: e.javaClass.simpleName}")
                .apply()
            false
        }
    }

    private fun scriptFor(action: BotAction): String = when (action) {
        BotAction.START -> """
            set -u
            BOT_DIR="${'$'}HOME/Robo-Nicolas"
            STATE_DIR="${'$'}HOME/.nicolas-keep-alive"
            PID_FILE="${'$'}STATE_DIR/bot.pid"
            LOG_FILE="${'$'}STATE_DIR/bot.log"
            mkdir -p "${'$'}STATE_DIR"
            if [ ! -f "${'$'}BOT_DIR/start.sh" ]; then
                echo "ERRO: não encontrei ~/Robo-Nicolas/start.sh"
                echo "BOT_PROCESS=STOPPED"
                exit 20
            fi
            if [ -s "${'$'}PID_FILE" ]; then
                OLD_PID="$(cat "${'$'}PID_FILE" 2>/dev/null || true)"
                if [ -n "${'$'}OLD_PID" ] && kill -0 "${'$'}OLD_PID" 2>/dev/null; then
                    echo "BOT_PROCESS=RUNNING"
                    if grep -Fq "Autenticado e conectado ao WhatsApp" "${'$'}LOG_FILE" 2>/dev/null; then
                        echo "WHATSAPP=CONNECTED"
                    else
                        echo "WHATSAPP=CONNECTING"
                    fi
                    exit 0
                fi
            fi
            : > "${'$'}LOG_FILE"
            cd "${'$'}BOT_DIR" || exit 21
            nohup bash "${'$'}BOT_DIR/start.sh" >>"${'$'}LOG_FILE" 2>&1 </dev/null &
            BOT_PID="${'$'}!"
            printf '%s\n' "${'$'}BOT_PID" > "${'$'}PID_FILE"
            sleep 5
            if kill -0 "${'$'}BOT_PID" 2>/dev/null; then
                echo "BOT_PROCESS=RUNNING"
                if grep -Fq "Autenticado e conectado ao WhatsApp" "${'$'}LOG_FILE"; then
                    echo "WHATSAPP=CONNECTED"
                else
                    echo "WHATSAPP=CONNECTING"
                fi
                exit 0
            fi
            rm -f "${'$'}PID_FILE"
            echo "BOT_PROCESS=STOPPED"
            echo "WHATSAPP=UNKNOWN"
            tail -n 25 "${'$'}LOG_FILE" 2>/dev/null || true
            exit 22
        """.trimIndent()

        BotAction.STOP -> """
            set +e
            BOT_DIR="${'$'}HOME/Robo-Nicolas"
            STATE_DIR="${'$'}HOME/.nicolas-keep-alive"
            PID_FILE="${'$'}STATE_DIR/bot.pid"
            if [ -f "${'$'}BOT_DIR/stop.sh" ]; then
                cd "${'$'}BOT_DIR" && bash "${'$'}BOT_DIR/stop.sh"
                STOP_CODE="${'$'}?"
            else
                STOP_CODE=0
            fi
            if [ -s "${'$'}PID_FILE" ]; then
                BOT_PID="$(cat "${'$'}PID_FILE" 2>/dev/null)"
                if [ -n "${'$'}BOT_PID" ] && kill -0 "${'$'}BOT_PID" 2>/dev/null; then
                    kill -TERM "${'$'}BOT_PID" 2>/dev/null
                    sleep 2
                    kill -0 "${'$'}BOT_PID" 2>/dev/null && kill -KILL "${'$'}BOT_PID" 2>/dev/null
                fi
            fi
            rm -f "${'$'}PID_FILE"
            echo "BOT_PROCESS=STOPPED"
            echo "WHATSAPP=STOPPED"
            exit "${'$'}STOP_CODE"
        """.trimIndent()

        BotAction.RESTART -> """
            set +e
            BOT_DIR="${'$'}HOME/Robo-Nicolas"
            STATE_DIR="${'$'}HOME/.nicolas-keep-alive"
            PID_FILE="${'$'}STATE_DIR/bot.pid"
            mkdir -p "${'$'}STATE_DIR"
            if [ -f "${'$'}BOT_DIR/stop.sh" ]; then
                cd "${'$'}BOT_DIR" && bash "${'$'}BOT_DIR/stop.sh"
            fi
            if [ -s "${'$'}PID_FILE" ]; then
                BOT_PID="$(cat "${'$'}PID_FILE" 2>/dev/null)"
                [ -n "${'$'}BOT_PID" ] && kill -TERM "${'$'}BOT_PID" 2>/dev/null
            fi
            rm -f "${'$'}PID_FILE"
            sleep 2
            BOT_DIR="${'$'}HOME/Robo-Nicolas"
            LOG_FILE="${'$'}STATE_DIR/bot.log"
            if [ ! -f "${'$'}BOT_DIR/start.sh" ]; then
                echo "ERRO: não encontrei ~/Robo-Nicolas/start.sh"
                echo "BOT_PROCESS=STOPPED"
                exit 20
            fi
            : > "${'$'}LOG_FILE"
            cd "${'$'}BOT_DIR" || exit 21
            nohup bash "${'$'}BOT_DIR/start.sh" >>"${'$'}LOG_FILE" 2>&1 </dev/null &
            BOT_PID="${'$'}!"
            printf '%s\n' "${'$'}BOT_PID" > "${'$'}PID_FILE"
            sleep 5
            if kill -0 "${'$'}BOT_PID" 2>/dev/null; then
                echo "BOT_PROCESS=RUNNING"
                if grep -Fq "Autenticado e conectado ao WhatsApp" "${'$'}LOG_FILE"; then
                    echo "WHATSAPP=CONNECTED"
                else
                    echo "WHATSAPP=CONNECTING"
                fi
                exit 0
            fi
            rm -f "${'$'}PID_FILE"
            echo "BOT_PROCESS=STOPPED"
            tail -n 25 "${'$'}LOG_FILE" 2>/dev/null || true
            exit 22
        """.trimIndent()

        BotAction.STATUS -> """
            STATE_DIR="${'$'}HOME/.nicolas-keep-alive"
            PID_FILE="${'$'}STATE_DIR/bot.pid"
            LOG_FILE="${'$'}STATE_DIR/bot.log"
            BOT_PID=""
            [ -s "${'$'}PID_FILE" ] && BOT_PID="$(cat "${'$'}PID_FILE" 2>/dev/null)"
            if [ -n "${'$'}BOT_PID" ] && kill -0 "${'$'}BOT_PID" 2>/dev/null; then
                echo "BOT_PROCESS=RUNNING"
                if grep -Fq "Autenticado e conectado ao WhatsApp" "${'$'}LOG_FILE" 2>/dev/null; then
                    echo "WHATSAPP=CONNECTED"
                else
                    echo "WHATSAPP=CONNECTING"
                fi
            else
                echo "BOT_PROCESS=STOPPED"
                echo "WHATSAPP=DISCONNECTED"
            fi
            if grep -Eiq "logged out|status code 401|invalid session|device removed" "${'$'}LOG_FILE" 2>/dev/null; then
                echo "WHATSAPP=AUTH_REQUIRED"
            fi
        """.trimIndent()

        BotAction.LOGS -> """
            LOG_FILE="${'$'}HOME/.nicolas-keep-alive/bot.log"
            if [ -f "${'$'}LOG_FILE" ]; then
                tail -n 40 "${'$'}LOG_FILE"
            else
                echo "Ainda não existe log de execução."
            fi
        """.trimIndent()
    }
}
