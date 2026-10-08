package com.foodpack.downtime.sync

import com.foodpack.downtime.data.HandoverEntry
import com.foodpack.downtime.data.HandoverType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Posts one shift handover (Morning or Evening) to the same Apps Script
 * Web App the downtime log and Notes already use, under action
 * "morningHandover" / "eveningHandover". Edits and deletes of an
 * already-synced entry use "updateMorningHandover" / "deleteMorningHandover"
 * (and the Evening equivalents) — the script finds the matching row by the
 * hidden Entry ID and updates or removes it in place, same pattern as the
 * Downtime Log.
 */
class HandoverUploader(private val webhookUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun upload(entry: HandoverEntry): Result<Unit> = post(toJson(entry))

    /** Re-sends an already-synced entry so the script overwrites its row in place. */
    fun update(entry: HandoverEntry): Result<Unit> {
        val action = if (entry.type == HandoverType.MORNING) "updateMorningHandover" else "updateEveningHandover"
        return post(toJson(entry).apply { put("action", action) })
    }

    /** Asks the script to delete the row matching this entry's ID. */
    fun delete(entry: HandoverEntry): Result<Unit> {
        val action = if (entry.type == HandoverType.MORNING) "deleteMorningHandover" else "deleteEveningHandover"
        val json = JSONObject().apply {
            put("action", action)
            put("id", entry.id)
        }
        return post(json)
    }

    private fun post(json: JSONObject): Result<Unit> {
        if (webhookUrl.isBlank()) {
            return Result.failure(IllegalStateException("No Google Sheet link configured yet (see Settings)."))
        }
        return try {
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(webhookUrl).post(body).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Server responded ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toJson(e: HandoverEntry): JSONObject = JSONObject().apply {
        put("action", if (e.type == HandoverType.MORNING) "morningHandover" else "eveningHandover")
        put("id", e.id)
        put("date", e.date)
        put("operator", e.operatorName)

        put("sheIncident", e.sheIncident)
        put("sheIncidentDetail", e.sheIncidentDetail)
        put("technicalIssue", e.technicalIssue)
        put("technicalDetail", e.technicalDetail)
        put("qualityIssue", e.qualityIssue)
        put("qualityDetail", e.qualityDetail)
        put("downtimeIssue", e.downtimeIssue)
        put("downtimeDetail", e.downtimeDetail)
        put("todaysVolume", e.todaysVolume ?: JSONObject.NULL)
        put("tomorrowsVolume", e.tomorrowsVolume ?: JSONObject.NULL)

        put("teamAPlanned", e.teamAPlanned ?: JSONObject.NULL)
        put("teamAActual", e.teamAActual ?: JSONObject.NULL)
        put("teamAFinishPlanned", e.teamAFinishPlanned)
        put("teamAFinishActual", e.teamAFinishActual)
        put("teamBPlanned", e.teamBPlanned ?: JSONObject.NULL)
        put("teamBActual", e.teamBActual ?: JSONObject.NULL)
        put("teamBFinishPlanned", e.teamBFinishPlanned)
        put("teamBFinishActual", e.teamBFinishActual)
        put("teamCPlanned", e.teamCPlanned ?: JSONObject.NULL)
        put("teamCActual", e.teamCActual ?: JSONObject.NULL)
        put("teamCFinishPlanned", e.teamCFinishPlanned)
        put("teamCFinishActual", e.teamCFinishActual)

        put("packingPpl", e.packingPpl ?: JSONObject.NULL)
        put("packingTrainees", e.packingTrainees ?: JSONObject.NULL)

        put("absence", e.absence)
        put("absenceNames", e.absenceNames)
        put("rtw", e.rtw)
        put("rtwNames", e.rtwNames)
        put("notOnRota", e.notOnRota)
        put("notOnRotaNames", e.notOnRotaNames)

        put("riskToService", e.riskToService)
        put("riskToServiceDetail", e.riskToServiceDetail)

        put("agencyCollation", e.agencyCollation ?: JSONObject.NULL)
        put("agencyPrep", e.agencyPrep ?: JSONObject.NULL)
        put("agencyHygiene", e.agencyHygiene ?: JSONObject.NULL)
        put("agencyFridge", e.agencyFridge ?: JSONObject.NULL)
        put("agencyPackagingStore", e.agencyPackagingStore ?: JSONObject.NULL)

        put("accidentIncident", e.accidentIncident)
        put("accidentIncidentDetail", e.accidentIncidentDetail)
        put("serviceScore", e.serviceScore ?: JSONObject.NULL)
        put("efficiency", e.efficiency ?: JSONObject.NULL)

        put("teamAFinishedTime", e.teamAFinishedTime)
        put("teamBFinishedTime", e.teamBFinishedTime)
        put("teamCFinishedTime", e.teamCFinishedTime)
        put("collationFinished", e.collationFinished)
        put("trayUpSentBy", e.trayUpSentBy)

        put("shorts", e.shorts)
        put("shortsDetail", e.shortsDetail)
        put("reportUpdated", e.reportUpdated)
        put("reportUpdatedDetail", e.reportUpdatedDetail)
        put("aob", e.aob)
        put("aobDetail", e.aobDetail)
    }
}
