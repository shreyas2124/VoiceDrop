package com.example.voicedrop.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start(outputFile: File) {
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(320_000)
            setAudioSamplingRate(48_000)
            setAudioChannels(1)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        recorder = mr
        currentFile = outputFile
    }

    fun pause() {
        recorder?.pause()
    }

    fun resume() {
        recorder?.resume()
    }

    fun stop(): File {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        val file = currentFile ?: throw IllegalStateException("No recording in progress")
        currentFile = null
        return file
    }

    fun release() {
        recorder?.release()
        recorder = null
    }
}
