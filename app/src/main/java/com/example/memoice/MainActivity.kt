package com.example.memoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.memoice.navigation.SetupNavGraph
import com.example.memoice.recorder.AudioRecorder
import com.example.memoice.ui.theme.MemoiceTheme
import java.io.File

class MainActivity : ComponentActivity() {

    lateinit var navController: NavHostController
    var recorder: AudioRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recorder = AudioRecorder(this)

        val folder = File(this.filesDir, "Recs")
        if (!folder.exists()) { folder.mkdir() }

        setContent {
            MemoiceTheme {
                navController = rememberNavController()
                SetupNavGraph(
                    navController = navController,
                    recorder = recorder!!,
                    folder = folder
                )
            }
        }
    }
}