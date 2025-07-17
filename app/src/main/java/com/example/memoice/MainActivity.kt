package com.example.memoice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.memoice.recorder.AudioRecorder
import com.example.memoice.service.AudioPlayer
import com.example.memoice.ui.theme.MemoiceTheme
import com.example.memoice.navigation.SetupNavGraph
import com.example.memoice.service.AudioPlayerViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    lateinit var navController: NavHostController

    var recorder: AudioRecorder? = null

    private val viewModel: AudioPlayerViewModel by viewModels()

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("CONNECTION", "Si l'ho chiamata")
            val binder = service as AudioPlayer.mBinder
            viewModel.setMyService(binder.getBoundPlayer())
            viewModel.setBoundStatus(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.setBoundStatus(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorder = AudioRecorder(this)

        val intent = Intent(this, AudioPlayer::class.java)
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE)

        val folder = File(this.filesDir, "Recs")
        if (!folder.exists()) { folder.mkdir() }

        setContent {
            MemoiceTheme {
                navController = rememberNavController()
                SetupNavGraph(
                    navController = navController,
                    recorder = recorder!!,
                    service = viewModel,
                    folder = folder
                )
            }
        }
    }

    /* TODO: Gestione onPause(), onStop(), ... */
    /* TODO: Landscape */

    /*override fun onPause() {
        super.onPause()
    }*/

    /*override fun onResume() {
        super.onResume()
    }*/

    override fun onDestroy() {
        super.onDestroy()
        if(viewModel.isBound()) {
            unbindService(myConnection)
            viewModel.setBoundStatus(false)
        }
    }
}