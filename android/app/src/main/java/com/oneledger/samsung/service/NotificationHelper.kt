package com.oneledger.samsung.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.oneledger.samsung.MainActivity
import com.oneledger.samsung.R

object NotificationHelper {
    const val CHANNEL_AUTO_RECORD = "oneledger_auto_record"
    const val CHANNEL_DAILY_REMINDER = "oneledger_daily_reminder"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val autoChannel = NotificationChannel(
            CHANNEL_AUTO_RECORD,
            "自動記帳與去敏通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "偵測到消費推播或循環記帳自動入帳時即時通知"
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_DAILY_REMINDER,
            "每日記帳提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每日提醒「今天記帳了嗎？」"
        }

        manager.createNotificationChannel(autoChannel)
        manager.createNotificationChannel(reminderChannel)
    }

    fun sendAutoRecordNotification(context: Context, title: String, body: String) {
        send(context, CHANNEL_AUTO_RECORD, title, body, (System.currentTimeMillis() % 100000).toInt())
    }

    fun sendDailyReminder(context: Context, title: String, body: String) {
        send(context, CHANNEL_DAILY_REMINDER, title, body, 9001)
    }

    private fun send(context: Context, channelId: String, title: String, body: String, notifId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }
}
