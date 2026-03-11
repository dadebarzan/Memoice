package com.example.memoice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memoice.recorder.AudioRecorder
import com.example.memoice.repository.MemoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecordViewModel(
    private val recorder: AudioRecorder,
    private val repository: MemoRepository // Usiamo lo stesso repository di prima
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordDuration = MutableStateFlow(0)
    val recordDuration: StateFlow<Int> = _recordDuration.asStateFlow()

    private var timerJob: Job? = null
    private var currentFile: File? = null

    fun getOutputFile(): File {
        val fileName =
            "Memo_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".m4a"
        return File(repository.folder, fileName)
    }

    fun startRecording(file: File) {
        currentFile = file
        recorder.start(file)
        _isRecording.value = true
        _recordDuration.value = 0
        
        timerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordDuration.value += 1
            }
        }
    }

    fun stopRecording() {
        if (_isRecording.value) {
            recorder.stop()
            timerJob?.cancel()
            _isRecording.value = false
        }
    }

    fun cancelRecording() {
        stopRecording()
        currentFile?.let {
            viewModelScope.launch {
                repository.deleteRecord(it)
            }
        }
    }

    fun onPause() {
        if (_isRecording.value) {
            stopRecording()
        }
    }
}

class RecordViewModelFactory(
    private val recorder: AudioRecorder,
    private val repository: MemoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordViewModel(recorder, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}