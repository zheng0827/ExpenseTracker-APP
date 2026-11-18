package com.oneledger.samsung.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oneledger.samsung.OneLedgerApp

class RecurringPostWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? OneLedgerApp ?: return Result.failure()
        app.repository.postDueRecurringRules()
        return Result.success()
    }
}
