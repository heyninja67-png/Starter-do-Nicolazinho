package com.nicolas.keepalive.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.nicolas.keepalive.termux.BotAction
import com.nicolas.keepalive.termux.TermuxBridge

class KeepAliveService : Service() {
    private val handler = Handler(Looper.getMainLooper())

    private val monitor = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val resultAt = prefs.getLong(TermuxBridge.KEY_LAST_RESULT_AT, 0L)
            val processRunning = prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)
            val wasConnected = prefs.getBoolean(TermuxBridge.KEY_WAS_CONNECTED, false)
            val autoRestart = prefs.getBoolean(TermuxBridge.KEY_AUTO_RESTART, true)
            val lastRestart = prefs.getLong(TermuxBridge.KEY_LAST_AUTO_RESTART_AT, 0L)

            // Usa somente o estado devolvido pelo Termux. Evita reiniciar antes da primeira resposta.
            if (autoRestart && wasConnected && !processRunning &&
                resultAt > 0L && now - resultAt < 120_000L &&
                now - lastRestart > 180_000L
            ) {
                prefs.edit().putLong(TermuxBridge.KEY_LAST_AUTO_RESTART_AT, now).apply()
                TermuxBridge.execute(this@KeepAliveService, BotAction.START)
            }

            TermuxBridge.execute(this@KeepAliveService, BotAction.STATUS)
            refreshNotification()
            handler.postDelayed(this, MONITOR_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Nicolas Keep Alive",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        startForeground(NOTIFICATION_ID, buildNotification("Preparando monitoramento"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> {
                getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE).edit()
                    .putBoolean(TermuxBridge.KEY_AUTO_RESTART, true)
                    .apply()
                TermuxBridge.execute(this, BotAction.START)
            }
            ACTION_RESTART -> TermuxBridge.execute(this, BotAction.RESTART)
            ACTION_STOP -> {
                getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE).edit()
                    .putBoolean(TermuxBridge.KEY_AUTO_RESTART, false)
                    .apply()
                TermuxBridge.execute(this, BotAction.STOP)
                handler.removeCallbacks(monitor)
                handler.postDelayed({
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }, 1500L)
                return START_NOT_STICKY
            }
        }

        handler.removeCallbacks(monitor)
        handler.postDelayed(monitor, MONITOR_INTERVAL_MS)
        return START_STICKY
    }

    private fun refreshNotification() {
        val prefs = getSharedPreferences(TermuxBridge.PREFS, MODE_PRIVATE)
        val processRunning = prefs.getBoolean(TermuxBridge.KEY_PROCESS_RUNNING, false)
        val connected = prefs.getBoolean(TermuxBridge.KEY_WHATSAPP_CONNECTED, false)
        val text = when {
            connected -> "Robô Nicolas ativo • WhatsApp conectado"
            processRunning -> "Robô Nicolas em execução • verificando WhatsApp"
            else -> "Monitorando • aguardando resposta do Termux"
        }
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(message: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Nicolas Keep Alive")
            .setContentText(message)
            .setSmallIcon(com.nicolas.keepalive.R.drawable.ic_launcher)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.nicolas.keepalive.START"
        const val ACTION_STOP = "com.nicolas.keepalive.STOP"
        const val ACTION_RESTART = "com.nicolas.keepalive.RESTART"
        private const val CHANNEL_ID = "nicolas_keep_alive"
        private const val NOTIFICATION_ID = 1001
        private const val MONITOR_INTERVAL_MS = 60_000L
    }
}
