package com.example.voicedrop.ui.main

enum class RecordingState {
    IDLE,       // Nothing recorded yet / after save is complete
    RECORDING,  // MediaRecorder is actively capturing audio
    PAUSED,     // MediaRecorder is paused mid-recording
    STOPPED,    // MediaRecorder stopped, file ready to be named and saved
}
