package com.example.voicedrop.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.voicedrop.data.db.AppDatabase
import com.example.voicedrop.data.model.RecordingEntry
import com.example.voicedrop.data.model.UploadStatus
import com.example.voicedrop.data.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordingRepository by lazy {
        val dao = AppDatabase.getInstance(application).recordingDao()
        val wm  = WorkManager.getInstance(application)
        RecordingRepository(dao, wm, application)
    }

    val recordings: Flow<List<RecordingEntry>> = repository.recordings

    val pendingCount: Flow<Int> = recordings.map { list ->
        list.count { it.uploadStatus == UploadStatus.PENDING }
    }

    val failedCount: Flow<Int> = recordings.map { list ->
        list.count { it.uploadStatus == UploadStatus.FAILED }
    }

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun retryAllFailed() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.retryAllFailed()
                _snackbarMessage.value = "Retrying all failed uploads."
            } catch (e: Exception) {
                _snackbarMessage.value = "Retry failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryUpload(id: Long) {
        viewModelScope.launch {
            try {
                repository.retryUpload(id)
                _snackbarMessage.value = "Upload queued."
            } catch (e: Exception) {
                _snackbarMessage.value = "Could not queue upload: ${e.localizedMessage}"
            }
        }
    }

    fun deleteRecording(entry: RecordingEntry) {
        viewModelScope.launch {
            try {
                repository.deleteRecording(entry)
            } catch (e: Exception) {
                _snackbarMessage.value = "Delete failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.clearAll()
                _snackbarMessage.value = "History cleared."
            } catch (e: Exception) {
                _snackbarMessage.value = "Clear failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
