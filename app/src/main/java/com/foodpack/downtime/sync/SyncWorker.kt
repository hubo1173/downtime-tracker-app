package com.foodpack.downtime.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foodpack.downtime.data.AppDatabase
import com.foodpack.downtime.data.SettingsStore
import kotlinx.coroutines.flow.first

/**
 * Runs in the background (on schedule and whenever triggered right after
 * a save/edit/delete) and pushes every pending change to the Google
 * Sheet: new downtime entries, edits to already-synced ones, deletes,
 * and shift handovers. Safe to run repeatedly — rows already up to date
 * are skipped.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val dowDao = db.downtimeDao()
        val handoverDao = db.handoverDao()
        val webhookUrl = SettingsStore(applicationContext).webhookUrl.first()

        if (webhookUrl.isBlank()) {
            // Nothing configured yet — not a failure, just nothing to do.
            return Result.success()
        }

        val uploader = SheetsUploader(webhookUrl)
        var allOk = true

        // Deletes first, so a row that was both edited and then deleted
        // before its edit ever synced doesn't get re-created.
        for (entry in dowDao.getPendingDeletes()) {
            if (!entry.sheetRowCreated) {
                // Never made it to the Sheet in the first place — nothing to undo there.
                dowDao.delete(entry)
                continue
            }
            val result = uploader.delete(entry.id)
            if (result.isSuccess) {
                dowDao.delete(entry)
            } else {
                allOk = false
                dowDao.markSyncFailed(entry.id, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }

        for (entry in dowDao.getUnsynced()) {
            val result = if (entry.sheetRowCreated) uploader.update(entry) else uploader.upload(entry)
            if (result.isSuccess) {
                dowDao.markSynced(entry.id)
            } else {
                allOk = false
                dowDao.markSyncFailed(entry.id, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }

        val handoverUploader = HandoverUploader(webhookUrl)

        for (entry in handoverDao.getPendingDeletes()) {
            if (!entry.sheetRowCreated) {
                handoverDao.delete(entry)
                continue
            }
            val result = handoverUploader.delete(entry)
            if (result.isSuccess) {
                handoverDao.delete(entry)
            } else {
                allOk = false
                handoverDao.markSyncFailed(entry.id, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }

        for (entry in handoverDao.getUnsynced()) {
            val result = if (entry.sheetRowCreated) handoverUploader.update(entry) else handoverUploader.upload(entry)
            if (result.isSuccess) {
                handoverDao.markSynced(entry.id)
            } else {
                allOk = false
                handoverDao.markSyncFailed(entry.id, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }

        return if (allOk) Result.success() else Result.retry()
    }
}
