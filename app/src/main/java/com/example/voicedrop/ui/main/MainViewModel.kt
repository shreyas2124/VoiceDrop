package com.example.voicedrop.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.voicedrop.data.db.AppDatabase
import com.example.voicedrop.data.repository.RecordingRepository
import com.example.voicedrop.recorder.AudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val recorder = AudioRecorder(application)

    private val repository: RecordingRepository by lazy {
        val dao = AppDatabase.getInstance(application).recordingDao()
        val wm  = WorkManager.getInstance(application)
        RecordingRepository(dao, wm, application)
    }

    // ─── Public state ──────────────────────────────────────────────────────────

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    /** Elapsed recording time in whole seconds. Paused time is preserved. */
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    /** True while the ZIP is being created / uploaded in the background. */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** One-shot messages for the Snackbar. Null when nothing to show. */
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // ─── Private state ────────────────────────────────────────────────────────

    private var timerJob: Job? = null
    private var tempM4aFile: File? = null

    // ─── Recording controls ───────────────────────────────────────────────────

    fun startRecording() {
        val recordingsDir = File(getApplication<Application>().filesDir, "recordings").also { it.mkdirs() }
        val outFile = File(recordingsDir, "tmp_${System.currentTimeMillis()}.m4a")

        try {
            recorder.start(outFile)
            tempM4aFile = outFile
            _recordingState.value = RecordingState.RECORDING
            _elapsedSeconds.value = 0L
            startTimer()
        } catch (e: Exception) {
            _snackbarMessage.value = "Could not start recording: ${e.localizedMessage}"
        }
    }

    fun pauseRecording() {
        try {
            recorder.pause()
            _recordingState.value = RecordingState.PAUSED
            timerJob?.cancel()
        } catch (e: Exception) {
            _snackbarMessage.value = "Pause failed: ${e.localizedMessage}"
        }
    }

    fun resumeRecording() {
        try {
            recorder.resume()
            _recordingState.value = RecordingState.RECORDING
            startTimer()
        } catch (e: Exception) {
            _snackbarMessage.value = "Resume failed: ${e.localizedMessage}"
        }
    }

    fun stopRecording() {
        try {
            recorder.stop()
            timerJob?.cancel()
            _recordingState.value = RecordingState.STOPPED
        } catch (e: Exception) {
            _snackbarMessage.value = "Stop failed: ${e.localizedMessage}"
            _recordingState.value = RecordingState.IDLE
            _elapsedSeconds.value = 0L
        }
    }

    /** Called after the user enters their name in the save dialog. */
    fun saveRecording(name: String) {
        val file = tempM4aFile ?: run {
            _snackbarMessage.value = "No recording found to save."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveRecording(name.trim(), file)
                _snackbarMessage.value = "Recording saved — uploading in background."
                resetState()
            } catch (e: Exception) {
                _snackbarMessage.value = "Save failed: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _elapsedSeconds.value++
            }
        }
    }

    private fun resetState() {
        _recordingState.value = RecordingState.IDLE
        _elapsedSeconds.value = 0L
        tempM4aFile = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        recorder.release()
    }
}
