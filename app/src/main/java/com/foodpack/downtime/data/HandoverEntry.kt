package com.foodpack.downtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HandoverType { MORNING, EVENING }

/**
 * One shift handover — either a Start of Shift (Morning Update) or an
 * End of Shift (End of Shift Update). Both share this one table; only
 * the fields relevant to `type` are expected to be filled in.
 */
@Entity(tableName = "handover_entries")
data class HandoverEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val type: HandoverType,
    val date: String,             // yyyy-MM-dd
    val operatorName: String,
    val createdAt: Long,

    // ---- Morning (Start of Shift) ----
    val sheIncident: Boolean = false,
    val sheIncidentDetail: String = "",
    val technicalIssue: Boolean = false,
    val technicalDetail: String = "",
    val todaysVolume: Int? = null,
    val tomorrowsVolume: Int? = null,

    val teamAPlanned: Int? = null,
    val teamAActual: Int? = null,
    val teamAFinishPlanned: String = "",
    val teamAFinishActual: String = "",
    val teamBPlanned: Int? = null,
    val teamBActual: Int? = null,
    val teamBFinishPlanned: String = "",
    val teamBFinishActual: String = "",
    val teamCPlanned: Int? = null,
    val teamCActual: Int? = null,
    val teamCFinishPlanned: String = "",
    val teamCFinishActual: String = "",

    val packingPpl: Int? = null,
    val packingTrainees: Int? = null,

    val absence: Boolean = false,
    val absenceNames: String = "",
    val rtw: Boolean = false,
    val rtwNames: String = "",
    val notOnRota: Boolean = false,
    val notOnRotaNames: String = "",

    val riskToService: Boolean = false,
    val riskToServiceDetail: String = "",

    val agencyCollation: Int? = null,
    val agencyPrep: Int? = null,
    val agencyHygiene: Int? = null,
    val agencyFridge: Int? = null,
    val agencyPackagingStore: Int? = null,

    // ---- Shared between Morning and Evening ----
    val qualityIssue: Boolean = false,
    val qualityDetail: String = "",
    val downtimeIssue: Boolean = false,
    val downtimeDetail: String = "",

    // ---- Evening (End of Shift) ----
    val accidentIncident: Boolean = false,
    val accidentIncidentDetail: String = "",
    val serviceScore: Int? = null,
    val efficiency: Int? = null,

    val teamAFinishedTime: String = "",
    val teamBFinishedTime: String = "",
    val teamCFinishedTime: String = "",
    val collationFinished: String = "",
    val trayUpSentBy: String = "",

    val shorts: Boolean = false,
    val shortsDetail: String = "",
    val reportUpdated: Boolean = false,
    val reportUpdatedDetail: String = "",
    val aob: Boolean = false,
    val aobDetail: String = "",

    val synced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val sheetRowCreated: Boolean = false,
    val pendingDelete: Boolean = false
)
