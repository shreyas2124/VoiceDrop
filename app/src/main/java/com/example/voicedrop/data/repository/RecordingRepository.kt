package com.example.voicedrop.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.voicedrop.data.db.RecordingDao
import com.example.voicedrop.data.model.RecordingEntry
import com.example.voicedrop.data.model.UploadStatus
import com.example.voicedrop.data.worker.UploadWorker
import com.example.voicedrop.util.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RecordingRepository(
    private val dao: RecordingDao,
    private val workManager: WorkManager,
    private val context: Context
) {

    val recordings: Flow<List<RecordingEntry>> = dao.getAll()

    suspend fun saveRecording(userName: String, tempM4aFile: File): Long {
        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(timestamp))
        val cleanName = userName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "${cleanName}_${dateStr}.m4a"

        val recordingsDir = File(context.filesDir, "recordings")
        recordingsDir.mkdirs()
        val finalM4aFile = File(recordingsDir, fileName)

        withContext(Dispatchers.IO) {
            val renamed = tempM4aFile.renameTo(finalM4aFile)
            if (!renamed) {
                tempM4aFile.copyTo(finalM4aFile, overwrite = true)
                tempM4aFile.delete()
            }
        }

        val zipsDir = File(context.filesDir, "zips")
        val zipFile = withContext(Dispatchers.IO) {
            ZipManager.compress(finalM4aFile, zipsDir)
        }

        val entry = RecordingEntry(
            userName = userName,
            fileName = fileName,
            filePath = finalM4aFile.absolutePath,
            zipPath = zipFile.absolutePath,
            dateTimeMillis = timestamp,
            uploadStatus = UploadStatus.PENDING
        )

        val id = dao.insert(entry)
        enqueueUpload(id)
        return id
    }

    fun enqueueUpload(id: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf("recording_id" to id)

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30L,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "upload_$id",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun retryUpload(id: Long) {
        dao.updateStatus(id, UploadStatus.PENDING)
        workManager.cancelUniqueWork("upload_$id")
        enqueueUpload(id)
    }

    suspend fun retryAllFailed() {
        dao.getFailedEntries().forEach { retryUpload(it.id) }
    }

    suspend fun deleteRecording(entry: RecordingEntry) {
        withContext(Dispatchers.IO) {
            runCatching { File(entry.filePath).delete() }
            runCatching { File(entry.zipPath).delete() }
        }
        dao.delete(entry)
    }

    suspend fun clearAll() {
        val allEntries = dao.getAll().first()
        withContext(Dispatchers.IO) {
            allEntries.forEach { entry ->
                runCatching { File(entry.filePath).delete() }
                runCatching { File(entry.zipPath).delete() }
            }
        }
        dao.deleteAll()
    }
}
