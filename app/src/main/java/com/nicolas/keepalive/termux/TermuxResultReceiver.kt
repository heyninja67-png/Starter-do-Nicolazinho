package com.nicolas.keepalive.termux

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = intent.getBundleExtra("result")
        if (result == null) {
            saveFailure(context, "Termux não devolveu o resultado. Confira allow-external-apps=true e a permissão RUN_COMMAND.")
            return
        }

        val stdout = result.getString("stdout").orEmpty()
        val stderr = result.getString("stderr").orEmpty()
        val errorMessage = result.getString("errmsg").orEmpty()
        val internalError = result.getInt("err", Activity.RESULT_OK)
        val exitCode = result.getInt("exitCode", -1)
        val action = intent.getStringExtra(EXTRA_ACTION).orEmpty()
        val combined = listOf(stdout, stderr)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .takeLast(5000)

        val running = stdout.contains("BOT_PROCESS=RUNNING")
        val connected = stdout.contains("WHATSAPP=CONNECTED")
        val authRequired = stdout.contains("WHATSAPP=AUTH_REQUIRED")
        val prefs = context.getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val edit = prefs.edit()
            .putBoolean(TermuxBridge.KEY_PROCESS_RUNNING, running)
            .putBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, connected)
            .putLong(TermuxBridge.KEY_LAST_RESULT_AT, now)
            .putString(TermuxBridge.KEY_LAST_ACTION, action)
            .putString("last_command_output", combined)

        if (connected) edit.putBoolean(TermuxBridge.KEY_WAS_CONNECTED, true)

        val message = when {
            errorMessage.isNotBlank() -> errorMessage
            internalError != Activity.RESULT_OK ->
                "Erro interno do Termux ($internalError). ${combined.take(800)}"
            exitCode != 0 ->
                "Comando $action falhou (código $exitCode). ${combined.take(1200)}"
            authRequired ->
                "A sessão do WhatsApp precisa de atenção. O app preservou auth/ e não iniciou novo pareamento."
            connected -> "Robô Nicolas em execução e WhatsApp conectado."
            running -> "Robô Nicolas em execução; aguardando/reconectando o WhatsApp."
            action == BotAction.STOP.name -> "Robô parado. Sessão e arquivos do WhatsApp preservados."
            combined.isNotBlank() -> combined.take(1200)
            else -> "Comando $action concluído."
        }
        edit.putString(TermuxBridge.KEY_LAST_MESSAGE, message).apply()

        // Recupera o supervisor se ele caiu depois de uma conexão válida. O start-bg.sh
        // usa a sessão existente e recusa iniciar quando auth/creds.json não existe.
        val shouldRestart = action == BotAction.STATUS.name &&
            exitCode == 0 &&
            !running &&
            !authRequired &&
            prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, false) &&
            prefs.getBoolean(TermuxBridge.KEY_WAS_CONNECTED, false) &&
            now - prefs.getLong(TermuxBridge.KEY_LAST_AUTO_RESTART_AT, 0L) > 180_000L

        if (shouldRestart) {
            prefs.edit().putLong(TermuxBridge.KEY_LAST_AUTO_RESTART_AT, now).apply()
            TermuxBridge.execute(context, BotAction.START)
        }
    }

    private fun saveFailure(context: Context, message: String) {
        context.getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TermuxBridge.KEY_LAST_MESSAGE, message)
            .putLong(TermuxBridge.KEY_LAST_RESULT_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        const val EXTRA_ACTION = "bot_action"
        const val EXTRA_REQUEST_ID = "request_id"
    }
}
