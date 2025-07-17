package com.example.memoice.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.memoice.DetailScreen
import com.example.memoice.HomeScreen
import com.example.memoice.RecScreen
import com.example.memoice.recorder.AudioRecorder
import com.example.memoice.service.AudioPlayer
import com.example.memoice.service.AudioPlayerViewModel
import java.io.File

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    recorder: AudioRecorder,
    service: AudioPlayerViewModel,//AudioPlayer?,
    folder: File
) {
    NavHost(navController = navController,
            startDestination = Screen.Home.route
        ) {
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen(
                navController = navController,
                folder = folder
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument(KEY) {
                type = NavType.StringType
            })
        ) {
            DetailScreen(
                navController = navController,
                serviceViewModel = service,
                folder = folder,
                reference = it.arguments?.getString(KEY)!!
            )
        }
        composable(
            route = Screen.Rec.route,
            arguments = listOf(navArgument(KEY) {
                type = NavType.StringType
                defaultValue = ""
            })
        ) {
            RecScreen(
                navController = navController,
                recorder = recorder,
                folder = folder,
                reference = it.arguments?.getString(KEY)
            )
        }
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            activity.requestedOrientation = originalOrientation
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}