package com.foodpack.downtime.data

import android.content.Context
import com.foodpack.downtime.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Single place the UI talks to. Every write also nudges a background
 * sync attempt — if there's no signal right now, WorkManager will
 * catch it on its next periodic pass or the next syncNow() call.
 */
class DowntimeRepository(private val context: Context) {

    private val dao = AppDatabase.get(context).downtimeDao()
    val settings = SettingsStore(context)

    fun observeAll(): Flow<List<DowntimeEntry>> = dao.observeAll()
    fun observeOpenTimers(): Flow<List<DowntimeEntry>> = dao.observeOpenTimers()
    fun observeUnsyncedCount(): Flow<Int> = dao.observeUnsyncedCount()

    suspend fun startTimer(entry: DowntimeEntry): Long {
        val id = dao.insert(entry.copy(startTime = entry.createdAt, isOpen = true))
        SyncScheduler.syncNow(context)
        return id
    }

    suspend fun stopTimer(id: Long, endTime: Long) {
        val existing = dao.getById(id) ?: return
        val start = existing.startTime ?: existing.createdAt
        val minutes = ((endTime - start) / 60000L).toInt().coerceAtLeast(0)
        dao.update(
            existing.copy(
                endTime = endTime,
                durationMinutes = minutes,
                isOpen = false,
                synced = false
            )
        )
        SyncScheduler.syncNow(context)
    }

    suspend fun logInstant(entry: DowntimeEntry) {
        dao.insert(entry)
        SyncScheduler.syncNow(context)
    }

    suspend fun retrySync() {
        SyncScheduler.syncNow(context)
    }
}
