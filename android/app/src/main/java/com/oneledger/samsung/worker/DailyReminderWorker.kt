package com.oneledger.samsung.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oneledger.samsung.OneLedgerApp
import com.oneledger.samsung.service.NotificationHelper

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? OneLedgerApp ?: return Result.failure()
        if (!app.prefs.dailyReminderEnabled) return Result.success()

        val count = app.repository.getTodayTransactionCount()
        if (count == 0) {
            NotificationHelper.sendDailyReminder(
                context = applicationContext,
                title = "今天記帳了嗎？",
                body = "今天還沒有任何收支紀錄，花 5 秒鐘補記一筆吧。",
            )
        }
        return Result.success()
    }
}
