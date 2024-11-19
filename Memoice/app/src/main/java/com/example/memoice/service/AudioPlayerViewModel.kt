package com.example.memoice.service

import androidx.lifecycle.ViewModel

class AudioPlayerViewModel: ViewModel() {
    private var myService: AudioPlayer? = null
    private var isBound = false

    fun setMyService(service: AudioPlayer) {
        myService = service
    }

    fun getMyService(): AudioPlayer? {
        return myService
    }

    fun setBoundStatus(bound: Boolean) {
        isBound = bound
    }

    fun isBound(): Boolean {
        return isBound
    }
}