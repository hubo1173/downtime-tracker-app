package com.foodpack.downtime.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/** Downscales a camera photo and uploads it to the Drive photos folder via the Apps Script ("uploadPhoto"). */
class PhotoUploader(private val webhookUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .build()

    private fun compress(file: File, maxSide: Int = 1280): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        val bmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalStateException("Could not read photo")
        val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        return ByteArrayOutputStream().also { out.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
    }

    /** Returns the Drive URL of the uploaded photo. */
    fun upload(file: File, fileName: String): Result<String> {
        if (webhookUrl.isBlank()) {
            return Result.failure(IllegalStateException("No Google Sheet link configured yet (see Settings)."))
        }
        return try {
            val json = JSONObject().apply {
                put("action", "uploadPhoto")
                put("fileName", fileName)
                put("mimeType", "image/jpeg")
                put("base64Data", Base64.encodeToString(compress(file), Base64.NO_WRAP))
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(webhookUrl).post(body).build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) return Result.failure(Exception("Server responded ${response.code}"))
                val j = JSONObject(text)
                if (j.optString("status") == "ok") Result.success(j.optString("fileUrl"))
                else Result.failure(Exception(j.optString("message", "Photo upload failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
