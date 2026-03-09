package com.example.memoice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.os.PowerManager
import com.example.memoice.R
import kotlinx.coroutines.*

class AudioPlayer : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private var player: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("Memoice", "Memoice Player", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY" -> {
                val filePath = intent.getStringExtra("FILE_PATH") ?: return START_NOT_STICKY
                val title = intent.getStringExtra("TITLE") ?: "Memoice"
                
                // Se stiamo cercando di riprodurre lo stesso file già caricato ed è in pausa, riprendiamolo!
                if (AudioStateManager.currentFile.value == title && player != null) {
                    resume()
                } else {
                    play(filePath, title)
                }
            }
            "PAUSE" -> pause()
            "STOP" -> stop()
            "SEEK" -> {
                val progress = intent.getFloatExtra("PROGRESS", 0f)
                seekTo(progress)
            }
        }
        return START_NOT_STICKY
    }

    private fun play(filePath: String, title: String) {
        if (AudioStateManager.isPlaying.value) stop()

        AudioStateManager.setCurrentFile(title)
        AudioStateManager.setPlaying(true)
        AudioStateManager.setProgress(0f)

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(filePath)
            setOnPreparedListener(this@AudioPlayer)
            setOnCompletionListener(this@AudioPlayer)
            prepareAsync()
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        }

        val notification = Notification.Builder(applicationContext, "Memoice")
            .setContentTitle(title)
            .setContentText("Riproduzione in corso...")
            .setSmallIcon(R.drawable.graphic_eq)
            .build()
        
        startForeground(5786423, notification)
    }

    private fun pause() {
        player?.let {
            if (it.isPlaying) {
                it.pause() // Mette in pausa il MediaPlayer senza distruggerlo
                AudioStateManager.setPlaying(false)
                // Il loop della progress bar si fermerà da solo perché isPlaying diventa false
            }
        }
    }

    private fun seekTo(progress: Float) {
        player?.let {
            // Calcoliamo a quale millisecondo corrisponde la percentuale
            val position = (it.duration * progress).toInt()
            it.seekTo(position)
            AudioStateManager.setProgress(progress)
        }
    }

    private fun resume() {
        player?.let {
            it.start() // Riprende da dove era rimasto
            AudioStateManager.setPlaying(true)
            startProgressUpdates() // Facciamo ripartire la progress bar
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        startProgressUpdates()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        AudioStateManager.setProgress(1f)
        stop()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (player?.isPlaying == true) {
                val currentPos = player?.currentPosition?.toFloat() ?: 0f
                val totalDur = player?.duration?.toFloat() ?: 1f
                AudioStateManager.setProgress(currentPos / totalDur)
                delay(50)
            }
        }
    }

    private fun stop() {
        AudioStateManager.setPlaying(false)
        AudioStateManager.setCurrentFile(null)
        progressJob?.cancel()
        player?.release()
        player = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        stop()
        super.onDestroy()
    }
}