package com.example.memoice.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.memoice.DetailScreen
import com.example.memoice.HomeScreen
import com.example.memoice.RecScreen
import com.example.memoice.recorder.AudioRecorder
import com.example.memoice.repository.MemoRepository
import com.example.memoice.viewmodel.DetailViewModel
import com.example.memoice.viewmodel.DetailViewModelFactory
import com.example.memoice.viewmodel.HomeViewModel
import com.example.memoice.viewmodel.HomeViewModelFactory
import com.example.memoice.viewmodel.RecordViewModel
import com.example.memoice.viewmodel.RecordViewModelFactory
import java.io.File

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    recorder: AudioRecorder,
    folder: File
) {
    val application = LocalContext.current.applicationContext as android.app.Application

    NavHost(navController = navController,
            startDestination = Screen.Home.route
        ) {
        composable(
            route = Screen.Home.route
        ) {
            val repository = MemoRepository(folder)
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(repository)
            )

            HomeScreen(
                navController = navController,
                viewModel = homeViewModel
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument(KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            val repository = MemoRepository(folder)
            val detailViewModel: DetailViewModel = viewModel(
                factory = DetailViewModelFactory(application, repository)
            )

            val encodedReference = backStackEntry.arguments?.getString("reference") ?: ""
            val decodedReference = Uri.decode(encodedReference)

            DetailScreen(
                navController = navController,
                viewModel = detailViewModel,
                folder = folder,
                reference = decodedReference
            )
        }
        composable(
            route = Screen.Rec.route
        ) {
            val repository = MemoRepository(folder)
            val recordViewModel: RecordViewModel = viewModel(
                factory = RecordViewModelFactory(recorder, repository)
            )

            RecScreen(
                navController = navController,
                viewModel = recordViewModel
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