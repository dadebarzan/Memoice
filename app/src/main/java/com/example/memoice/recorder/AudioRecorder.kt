package com.example.memoice.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var path: String? = null

    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    fun start(outputFile: File) {
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
            setMaxDuration(30000)
            path = outputFile.absolutePath
            setOutputFile(path)

            prepare()
            start()

            isRecording = true
            recorder = this
        }
    }

    fun stop(delete: Boolean = false) {
        recorder?.stop()
        recorder?.reset()
        recorder?.release()
        if(delete) {
            File(path).delete()
        }
        isRecording = false
        path = null
        recorder = null
    }
}