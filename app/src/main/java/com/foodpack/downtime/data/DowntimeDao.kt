package com.foodpack.downtime.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DowntimeDao {

    @Insert
    suspend fun insert(entry: DowntimeEntry): Long

    @Update
    suspend fun update(entry: DowntimeEntry)

    @Delete
    suspend fun delete(entry: DowntimeEntry)

    @Query("SELECT * FROM downtime_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DowntimeEntry>>

    @Query("SELECT * FROM downtime_entries WHERE isOpen = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getOpenTimer(): DowntimeEntry?

    @Query("SELECT * FROM downtime_entries WHERE isOpen = 1 ORDER BY createdAt DESC")
    fun observeOpenTimers(): Flow<List<DowntimeEntry>>

    @Query("SELECT * FROM downtime_entries WHERE synced = 0 AND pendingDelete = 0 ORDER BY createdAt ASC")
    suspend fun getUnsynced(): List<DowntimeEntry>

    @Query("SELECT * FROM downtime_entries WHERE pendingDelete = 1 ORDER BY createdAt ASC")
    suspend fun getPendingDeletes(): List<DowntimeEntry>

    @Query("SELECT COUNT(*) FROM downtime_entries WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE downtime_entries SET synced = 1, sheetRowCreated = 1, lastSyncError = NULL WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE downtime_entries SET syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE id = :id")
    suspend fun markSyncFailed(id: Long, error: String)

    @Query("SELECT * FROM downtime_entries WHERE id = :id")
    suspend fun getById(id: Long): DowntimeEntry?
}
