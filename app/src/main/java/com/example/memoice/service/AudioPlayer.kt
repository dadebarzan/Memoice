package com.example.memoice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.net.toUri
import com.example.memoice.R

class AudioPlayer: Service(), MediaPlayer.OnPreparedListener {

    private val localBinder = mBinder()

    private var player: MediaPlayer? = null
    private var isPlaying = false

    inner class mBinder : Binder() {
        fun getBoundPlayer(): AudioPlayer {
            return this@AudioPlayer
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    override fun onCreate() {
        super.onCreate()

        val name: CharSequence = "Memoice Player"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel("Memoice", name, importance)
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    fun play(uri: Uri, title: String) {
        if (isPlaying) return
        isPlaying = true

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(applicationContext, uri)
            setOnPreparedListener(this@AudioPlayer)
            prepareAsync()
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        }

        val notificationBuilder: Notification.Builder = Notification.Builder(applicationContext, "Memoice")
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setSmallIcon(R.drawable.graphic_eq)
        val notification = notificationBuilder.build()
        val notificationID = 5786423
        startForeground(notificationID, notification)
    }

    fun isPlaying(): Boolean {
        return isPlaying
    }

    fun getCurrentPosition(): Float {
        if(isPlaying || player != null) {
            if(player!!.currentPosition == player!!.duration) {
                player!!.stop()
                return 1f
            }
            val total = player!!.duration.toFloat()
            val played = player!!.currentPosition.toFloat()
            return played/total
        }
        return 0f
    }

    fun stop() {
        if (isPlaying) {
            isPlaying = false
            player?.release()
            player = null
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
    }
}