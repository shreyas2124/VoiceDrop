package com.example.voicedrop.network

import android.util.Log
import com.example.voicedrop.data.model.RecordingEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class UploadRepository(private val api: UploadApi) {

    suspend fun upload(entry: RecordingEntry): Result<Unit> = runCatching {
        val zipFile = File(entry.zipPath)
        if (!zipFile.exists()) {
            Log.e("VoiceDrop", "ZIP file not found: ${entry.zipPath}")
            throw IOException("ZIP file not found: ${entry.zipPath}")
        }

        Log.d("VoiceDrop", "Preparing to upload ZIP: ${zipFile.absolutePath} (Size: ${zipFile.length()} bytes)")

        val namePart = entry.userName
            .toRequestBody("text/plain".toMediaType())

        val timestampPart = entry.dateTimeMillis.toString()
            .toRequestBody("text/plain".toMediaType())

        val filePart = MultipartBody.Part.createFormData(
            "zip_file",
            zipFile.name,
            zipFile.asRequestBody("application/zip".toMediaType())
        )

        Log.d("VoiceDrop", "Sending POST request to /api/upload...")
        val response = api.uploadRecording(namePart, timestampPart, filePart)
        
        Log.d("VoiceDrop", "Upload response code: ${response.code()}")

        if (!response.isSuccessful || response.body()?.success != true) {
            val errorMsg = response.body()?.message
                ?: "Upload failed: HTTP ${response.code()}"
            throw IOException(errorMsg)
        }
    }
}
