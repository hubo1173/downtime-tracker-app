package com.foodpack.downtime.sync

import com.foodpack.downtime.data.DowntimeEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Posts one entry as JSON to the Google Apps Script Web App URL
 * (see the Code.gs file shipped alongside this project). The script
 * appends a row to the Google Sheet and returns {"status":"ok"}.
 */
class SheetsUploader(private val webhookUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun upload(entry: DowntimeEntry): Result<Unit> = post(toJson(entry))

    /** Re-sends an already-synced entry under action "updateEntry" — the
     *  script finds the matching row by Entry ID and overwrites it in
     *  place instead of appending a duplicate. */
    fun update(entry: DowntimeEntry): Result<Unit> =
        post(toJson(entry).apply { put("action", "updateEntry") })

    /** Asks the script to delete the row matching this entry's ID. */
    fun delete(id: Long): Result<Unit> {
        val json = JSONObject().apply {
            put("action", "deleteEntry")
            put("id", id)
        }
        return post(json)
    }

    private fun post(json: JSONObject): Result<Unit> {
        if (webhookUrl.isBlank()) {
            return Result.failure(IllegalStateException("No Google Sheet link configured yet (see Settings)."))
        }
        return try {
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Server responded ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toJson(entry: DowntimeEntry): JSONObject {
        val created = Date(entry.createdAt)
        return JSONObject().apply {
            put("id", entry.id)
            put("date", dateFormat.format(created))
            put("time", timeFormat.format(created))
            put("operator", entry.operatorName)
            put("line", entry.line)
            put("station", entry.station)
            put("part", entry.part ?: "")
            put("subIssue", entry.subIssue ?: "")
            put("reason", entry.reason)
            put("note", entry.note)
            put("durationMinutes", entry.durationMinutes ?: JSONObject.NULL)
            put("peopleAffected", entry.peopleAffected ?: JSONObject.NULL)
            put("photoUrl", entry.photoUrl ?: "")
            put("startTime", entry.startTime?.let { timeFormat.format(Date(it)) } ?: "")
            put("endTime", entry.endTime?.let { timeFormat.format(Date(it)) } ?: "")
        }
    }
}
