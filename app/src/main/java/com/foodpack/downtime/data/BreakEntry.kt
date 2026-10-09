package com.foodpack.downtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One team's morning or afternoon break, with start/end times (free text, e.g. "09:30"). */
@Entity(tableName = "break_entries")
data class BreakEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val date: String,          // yyyy-MM-dd
    val operatorName: String,
    val team: String,          // "Team A" / "Team B" / "Team C"
    val breakType: String,     // "Morning" / "Afternoon"
    val startTime: String = "",
    val endTime: String = "",

    val synced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val sheetRowCreated: Boolean = false,
    val pendingDelete: Boolean = false
)
