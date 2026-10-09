package com.foodpack.downtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged event: a downtime stoppage OR a general issue report.
 * durationMinutes is null until the timer is stopped (or filled manually
 * for a quick, no-timer log).
 */
@Entity(tableName = "downtime_entries")
data class DowntimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val createdAt: Long,          // epoch millis, when the entry was created
    val operatorName: String,

    val line: String,             // e.g. "Line 3"
    val station: String,          // e.g. "Labeler"
    val part: String? = null,     // e.g. "Blade (glue build-up)" — labeler sub-part
    val subIssue: String? = null, // e.g. "Prep delivered late" — material sub-issue
    val reason: String,           // quick-reason chip text, or "Other" + note

    val note: String = "",

    val startTime: Long? = null,  // set when a live timer is started
    val endTime: Long? = null,    // set when the timer is stopped
    val durationMinutes: Int? = null, // final duration, timer-derived or manual
    val peopleAffected: Int? = null,  // how many people were stood idle / affected

    val isOpen: Boolean = false,  // true while a timer is running and not yet stopped

    val synced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,

    // true once a row for this entry has been written to the Sheet at least
    // once — after that, a re-sync (following an edit) updates that row in
    // place instead of appending a duplicate.
    val sheetRowCreated: Boolean = false,
    // true once the operator deletes this entry locally and it's waiting
    // for the matching Sheet row to be removed too (then the local row
    // itself is deleted).
    val pendingDelete: Boolean = false
)
