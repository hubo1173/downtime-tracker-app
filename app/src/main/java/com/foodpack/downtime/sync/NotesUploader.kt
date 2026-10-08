package com.foodpack.downtime.sync

import android.util.Base64
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One file listed inside a Notes subfolder in Drive. */
data class NoteFile(
    val name: String,
    val url: String,
    val mimeType: String,
    val updatedAt: String
)

/**
 * Talks to the same Apps Script Web App the downtime log uses (see
 * Code.gs), but for the Notes feature: uploading a reference file
 * (labeler settings, scale calibration sheet, etc.) into a fixed Drive
 * subfolder, and listing what's already there. No Google sign-in needed
 * on the phone — the script does the Drive work on our behalf, the same
 * way it already appends rows to the Sheet.
 */
class NotesUploader(private val webhookUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun uploadFile(folder: String, fileName: String, mimeType: String, bytes: ByteArray): Result<String> {
        if (webhookUrl.isBlank()) {
            return Result.failure(IllegalStateException("No Google Sheet link configured yet (see Settings)."))
        }
        return try {
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val json = JSONObject().apply {
                put("action", "uploadFile")
                put("folder", folder)
                put("fileName", fileName)
                put("mimeType", mimeType)
                put("base64Data", base64Data)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(webhookUrl).post(body).build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Server responded ${response.code}"))
                }
                val resultJson = JSONObject(text)
                if (resultJson.optString("status") == "ok") {
                    Result.success(resultJson.optString("fileUrl"))
                } else {
                    Result.failure(Exception(resultJson.optString("message", "Upload failed")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listFiles(folder: String): Result<List<NoteFile>> {
        if (webhookUrl.isBlank()) {
            return Result.failure(IllegalStateException("No Google Sheet link configured yet (see Settings)."))
        }
        return try {
            val base = webhookUrl.toHttpUrlOrNull()
                ?: return Result.failure(IllegalStateException("Invalid Web App URL."))
            val url = base.newBuilder()
                .addQueryParameter("action", "listFiles")
                .addQueryParameter("folder", folder)
                .build()
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Server responded ${response.code}"))
                }
                val resultJson = JSONObject(text)
                if (resultJson.optString("status") != "ok") {
                    return Result.failure(Exception(resultJson.optString("message", "Couldn't load files")))
                }
                val filesArray = resultJson.optJSONArray("files")
                val files = mutableListOf<NoteFile>()
                if (filesArray != null) {
                    for (i in 0 until filesArray.length()) {
                        val f = filesArray.getJSONObject(i)
                        files.add(
                            NoteFile(
                                name = f.optString("name"),
                                url = f.optString("url"),
                                mimeType = f.optString("mimeType"),
                                updatedAt = f.optString("updatedAt")
                            )
                        )
                    }
                }
                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
