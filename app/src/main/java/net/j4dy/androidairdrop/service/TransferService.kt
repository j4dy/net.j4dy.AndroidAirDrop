package net.j4dy.androidairdrop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.j4dy.androidairdrop.R

class TransferService : Service() {

    companion object {
        const val CHANNEL_ID = "airdrop_transfers"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "net.j4dy.androidairdrop.START_TRANSFER"
        const val ACTION_STOP = "net.j4dy.androidairdrop.STOP_TRANSFER"

        const val EXTRA_TARGET_NAME = "target_name"
        const val EXTRA_FILE_COUNT = "file_count"

        fun start(context: Context, targetName: String, fileCount: Int) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_NAME, targetName)
                putExtra(EXTRA_FILE_COUNT, fileCount)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val targetName = intent.getStringExtra(EXTRA_TARGET_NAME) ?: "Mac"
                val fileCount = intent.getIntExtra(EXTRA_FILE_COUNT, 1)
                startForeground(NOTIFICATION_ID, buildNotification(targetName, fileCount, 0))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AirDrop Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live file transfer progress to Mac"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(targetName: String, fileCount: Int, progressPercent: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sending to $targetName")
            .setContentText("Transferring $fileCount file(s)…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progressPercent, progressPercent == 0)
            .setOngoing(true)
            .build()
    }
}
