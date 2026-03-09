package com.example.memoice.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memoice.repository.MemoRepository
import com.example.memoice.service.AudioPlayer
import com.example.memoice.service.AudioStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DetailViewModel(
    private val app: Application,
    private val repository: MemoRepository
) : AndroidViewModel(app) {

    val isPlaying = AudioStateManager.isPlaying
    val progress = AudioStateManager.progress
    val currentFile = AudioStateManager.currentFile

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _fileDateInfo = MutableStateFlow(Pair("Caricamento...", "--:--"))
    val fileDateInfo: StateFlow<Pair<String, String>> = _fileDateInfo.asStateFlow()

    fun playAudio(file: File) {
        val intent = Intent(app, AudioPlayer::class.java).apply {
            action = "PLAY"
            putExtra("FILE_PATH", file.absolutePath)
            putExtra("TITLE", file.nameWithoutExtension)
        }
        // Avvia il Service in Foreground
        ContextCompat.startForegroundService(app, intent)
    }

    fun pauseAudio() {
        val intent = Intent(app, AudioPlayer::class.java).apply {
            action = "PAUSE"
        }
        app.startService(intent)
    }

    fun seekTo(progress: Float) {
        val intent = Intent(app, AudioPlayer::class.java).apply {
            action = "SEEK"
            putExtra("PROGRESS", progress)
        }
        app.startService(intent)
        
        // Aggiorniamo sùbito l'interfaccia per evitare ritardi visivi
        AudioStateManager.setProgress(progress)
    }

    fun stopAudio() {
        val intent = Intent(app, AudioPlayer::class.java).apply {
            action = "STOP"
        }
        app.startService(intent)

        resetProgress()
    }

    fun resetProgress() {
        AudioStateManager.setProgress(0f)
    }

    suspend fun renameFile(file: File, newName: String): Boolean = repository.renameRecord(file, newName)

    fun loadFileInfo(file: File) {
        viewModelScope.launch {
            _durationSeconds.value = repository.getDurationSeconds(file)

            _fileDateInfo.value = withContext(Dispatchers.IO) {
                try {
                    val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                    val millis = attrs.creationTime().toMillis()
                    val dateAndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                    val date = dateAndTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))
                    val time = dateAndTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    Pair(date, time)
                } catch (e: Exception) {
                    Pair("Sconosciuta", "--:--")
                }
            }
        }
    }
}

class DetailViewModelFactory(
    private val application: Application,
    private val repository: MemoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}