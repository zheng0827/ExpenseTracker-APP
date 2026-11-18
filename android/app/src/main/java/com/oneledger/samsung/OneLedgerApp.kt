package com.oneledger.samsung

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.oneledger.samsung.data.OneLedgerRepository
import com.oneledger.samsung.data.SecurePrefs
import com.oneledger.samsung.data.debt.DebtDatabase
import com.oneledger.samsung.data.ledger.LedgerDatabase
import com.oneledger.samsung.service.NotificationHelper
import com.oneledger.samsung.worker.CloudSyncWorker
import com.oneledger.samsung.worker.DailyReminderWorker
import com.oneledger.samsung.worker.RecurringPostWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class OneLedgerApp : Application() {
    lateinit var prefs: SecurePrefs
        private set
    lateinit var repository: OneLedgerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = SecurePrefs(this)
        val ledgerDb = LedgerDatabase.getInstance(this)
        val debtDb = DebtDatabase.getInstance(this)
        repository = OneLedgerRepository(this, ledgerDb, debtDb, prefs)

        NotificationHelper.createChannels(this)
        scheduleBackgroundWorkers()
    }

    fun scheduleBackgroundWorkers() {
        val wm = WorkManager.getInstance(this)

        // 1. 雲端 SQLite 背景同步（每 1 小時於有網路時自動同步）
        val syncReq = PeriodicWorkRequestBuilder<CloudSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        wm.enqueueUniquePeriodicWork("oneledger_cloud_sync", ExistingPeriodicWorkPolicy.KEEP, syncReq)

        // 2. 循環記帳自動到期入帳（每 12 小時檢查一次，同月冪等不重複）
        val recurringReq = PeriodicWorkRequestBuilder<RecurringPostWorker>(12, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork("oneledger_recurring_post", ExistingPeriodicWorkPolicy.KEEP, recurringReq)

        // 3. 每日記帳提醒（「今天記帳了嗎？」）
        rescheduleDailyReminder()
    }

    fun rescheduleDailyReminder() {
        val wm = WorkManager.getInstance(this)
        if (!prefs.dailyReminderEnabled) {
            wm.cancelUniqueWork("oneledger_daily_reminder")
            return
        }

        val now = LocalDateTime.now()
        var target = now.withHour(prefs.dailyReminderHour).withMinute(prefs.dailyReminderMinute).withSecond(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val initialDelayMinutes = Duration.between(now, target).toMinutes().coerceAtLeast(1)

        val reminderReq = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(
            "oneledger_daily_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderReq
        )
    }
}
