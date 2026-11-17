package com.nicolas.keepalive.termux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result: Bundle = intent.getBundleExtra("result") ?: run {
            saveFailure(context, "O Termux não devolveu o pacote de resultado.")
            return
        }

        val stdout = result.getString("stdout").orEmpty()
        val stderr = result.getString("stderr").orEmpty()
        val errorMessage = result.getString("errmsg").orEmpty()
        val exitCode = result.getInt("exitCode", -1)
        val action = intent.getStringExtra(EXTRA_ACTION).orEmpty()
        val combined = listOf(stdout, stderr, errorMessage)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .takeLast(3500)

        val running = stdout.contains("BOT_PROCESS=RUNNING") ||
            stdout.contains("BOT_PROCESS=STARTING")
        val connected = stdout.contains("WHATSAPP=CONNECTED")
        val prefs = context.getSharedPreferences(TermuxBridge.PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit()
            .putBoolean(TermuxBridge.KEY_PROCESS_RUNNING, running)
            .putBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, connected)
            .putLong(TermuxBridge.KEY_LAST_RESULT_AT, System.currentTimeMillis())
            .putString(TermuxBridge.KEY_LAST_ACTION, action)
            .putString(
                TermuxBridge.KEY_LAST_MESSAGE,
                when {
                    errorMessage.isNotBlank() -> errorMessage
                    exitCode != 0 -> "O comando $action terminou com código $exitCode."
                    combined.isBlank() -> "Comando $action concluído."
                    else -> combined
                }.take(1200)
            )
            .putString("last_command_output", combined)

        if (connected) edit.putBoolean(TermuxBridge.KEY_WAS_CONNECTED, true)
        if (action == BotAction.STOP.name) {
            edit.putBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)
                .putBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)
        }
        edit.apply()
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
