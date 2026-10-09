package com.foodpack.downtime.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakDao {
    @Insert
    suspend fun insert(entry: BreakEntry): Long

    @Update
    suspend fun update(entry: BreakEntry)

    @Delete
    suspend fun delete(entry: BreakEntry)

    @Query("SELECT * FROM break_entries WHERE pendingDelete = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BreakEntry>>

    @Query("SELECT * FROM break_entries WHERE synced = 0 AND pendingDelete = 0 ORDER BY createdAt ASC")
    suspend fun getUnsynced(): List<BreakEntry>

    @Query("SELECT * FROM break_entries WHERE pendingDelete = 1 ORDER BY createdAt ASC")
    suspend fun getPendingDeletes(): List<BreakEntry>

    @Query("UPDATE break_entries SET synced = 1, sheetRowCreated = 1, lastSyncError = NULL WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE break_entries SET syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE id = :id")
    suspend fun markSyncFailed(id: Long, error: String)
}
