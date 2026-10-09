package com.foodpack.downtime.sync

import com.foodpack.downtime.data.BreakEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Posts break times to the same Apps Script Web App ("breakLog" / "deleteBreak"). */
class BreakUploader(private val webhookUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun upload(e: BreakEntry): Result<Unit> = post(JSONObject().apply {
        put("action", "breakLog")
        put("id", e.id)
        put("date", e.date)
        put("operator", e.operatorName)
        put("team", e.team)
        put("breakType", e.breakType)
        put("startTime", e.startTime)
        put("endTime", e.endTime)
    })

    fun delete(e: BreakEntry): Result<Unit> = post(JSONObject().apply {
        put("action", "deleteBreak")
        put("id", e.id)
    })

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
}
