package com.oneledger.samsung.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oneledger.samsung.OneLedgerApp

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? OneLedgerApp ?: return Result.failure()
        val outcome = app.repository.syncWithCloudServer()
        return if (outcome.ok) Result.success() else Result.retry()
    }
}
