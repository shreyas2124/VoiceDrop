package com.example.voicedrop.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.voicedrop.data.db.AppDatabase
import com.example.voicedrop.data.model.UploadStatus
import com.example.voicedrop.network.RetrofitClient
import com.example.voicedrop.network.UploadRepository
import java.io.File

class UploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong("recording_id", -1L)
        if (recordingId == -1L) return Result.failure()

        val dao = AppDatabase.getInstance(applicationContext).recordingDao()

        return try {
            val entry = dao.getById(recordingId) ?: return Result.failure()

            Log.d("VoiceDrop", "Worker starting upload for file: ${entry.fileName}")

            val uploadRepo = UploadRepository(RetrofitClient.uploadApi)
            val uploadResult = uploadRepo.upload(entry)

            if (uploadResult.isSuccess) {
                Log.d("VoiceDrop", "Upload successful for: ${entry.fileName}")
                dao.updateStatus(recordingId, UploadStatus.SENT)
                runCatching { File(entry.zipPath).delete() }
                Result.success()
            } else {
                Log.e("VoiceDrop", "Upload failed for: ${entry.fileName}, attempt ${runAttemptCount + 1}/3", uploadResult.exceptionOrNull())
                if (runAttemptCount >= 2) {
                    dao.updateStatus(recordingId, UploadStatus.FAILED)
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceDrop", "Upload worker exception: ${e.message}", e)
            if (runAttemptCount >= 2) {
                dao.updateStatus(recordingId, UploadStatus.FAILED)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}
