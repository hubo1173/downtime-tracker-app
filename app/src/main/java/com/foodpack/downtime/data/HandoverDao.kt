package com.foodpack.downtime.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HandoverDao {

    @Insert
    suspend fun insert(entry: HandoverEntry): Long

    @Query("SELECT * FROM handover_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HandoverEntry>>

    @Query("SELECT * FROM handover_entries WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsynced(): List<HandoverEntry>

    @Query("UPDATE handover_entries SET synced = 1, lastSyncError = NULL WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE handover_entries SET syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE id = :id")
    suspend fun markSyncFailed(id: Long, error: String)
}
