package com.oneledger.samsung.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.oneledger.samsung.OneLedgerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 原生 Android 系統通知監聽服務：
 * 即時攔截使用者授權的銀行／行動支付 App 推播通知，
 * 先在本地端以 CardRedactor 遮蔽信用卡後四碼，再送 LLM 解析入帳並主動發送完成通知。
 */
class ExpenseNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // 避免監聽到自己發出的通知形成迴圈
        if (sbn.packageName == packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        if (title.isBlank() && text.isBlank()) return

        val appLabel = resolveAppLabel(sbn.packageName)
        val app = applicationContext as? OneLedgerApp ?: return

        serviceScope.launch {
            app.repository.handleIncomingNotification(
                appName = "$appLabel (${sbn.packageName})",
                title = title,
                body = text,
            )
        }
    }

    private fun resolveAppLabel(pkg: String): String {
        return try {
            val ai = packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
