package com.foodpack.downtime.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foodpack.downtime.data.AppDatabase
import com.foodpack.downtime.data.SettingsStore
import kotlinx.coroutines.flow.first

/**
 * Runs in the background (on schedule and whenever triggered right after
 * a save) and pushes every not-yet-synced row to the Google Sheet.
 * Safe to run repeatedly — rows already marked synced are skipped.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).downtimeDao()
        val webhookUrl = SettingsStore(applicationContext).webhookUrl.first()

        if (webhookUrl.isBlank()) {
            // Nothing configured yet — not a failure, just nothing to do.
            return Result.success()
        }

        val uploader = SheetsUploader(webhookUrl)
        val unsynced = dao.getUnsynced()
        var allOk = true

        for (entry in unsynced) {
            val result = uploader.upload(entry)
            if (result.isSuccess) {
                dao.markSynced(entry.id)
            } else {
                allOk = false
                dao.markSyncFailed(entry.id, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }

        return if (allOk) Result.success() else Result.retry()
    }
}
