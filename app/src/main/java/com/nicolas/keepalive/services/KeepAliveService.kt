package com.nicolas.keepalive.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager

class KeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel("nicolas_keep_alive", "Nicolas Keep Alive", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, "nicolas_keep_alive")
            .setContentTitle("Nicolas Keep Alive")
            .setContentText("Monitorando o Robô Nicolas")
            .setSmallIcon(com.nicolas.keepalive.R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(1001, notification)

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NicolasKeepAlive:monitor")
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
