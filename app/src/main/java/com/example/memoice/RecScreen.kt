package com.example.memoice

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.memoice.navigation.LockScreenOrientation
import com.example.memoice.navigation.Screen
import com.example.memoice.recorder.AudioRecorder
import com.example.memoice.ui.theme.AnimatedCircles
import com.example.memoice.ui.theme.DrawCircleOnCanvas
import com.example.memoice.ui.theme.MemoiceTheme
import com.example.memoice.ui.theme.dark_DeleteContainer
import com.example.memoice.ui.theme.dark_onDeleteContainer
import com.example.memoice.ui.theme.light_DeleteContainer
import com.example.memoice.ui.theme.light_onDeleteContainer
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun RecScreen(
    navController: NavController,
    recorder: AudioRecorder,
    folder: File,
    reference: String?
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val scope = rememberCoroutineScope()
    var created by remember { mutableStateOf(false) }
    var pressedRec by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf(0) }
    var lengthToAppend by remember { mutableStateOf("00") }
    var recTextIndicator by remember { mutableStateOf("Premi per\nregistrare") }
    if(pressedRec && recording) { recTextIndicator = "Registrazione\nin corso..." }
    if(pressedRec && !recording) { recTextIndicator = "Registrazione\ninterrotta!" }

    val fileName = "R_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddAAAAAAAA")).toString().dropLast(5) + ".mp3"

    val file = File(folder, fileName)
    if (!folder.exists()) {
        val isFolderCreated = folder.mkdir()
        if (!isFolderCreated) {
            navController.navigate(route = Screen.Home.route) {
                popUpTo(Screen.Home.route) {
                    inclusive = true
                }
            }
        }
    }
    try {
        if(!created) {
            file.createNewFile()
            created = true
            Log.d("DEBUG", "Creato file con nome: ${file.name}")
        }
    } catch (e: IOException) {
        navController.navigate(route = Screen.Home.route) {
            popUpTo(Screen.Home.route) {
                inclusive = true
            }
        }
    }

    if(length == 30) {
        recorder.stop()
        recording = false
        lengthToAppend = "30"
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_PAUSE) {
                if(pressedRec && recording) {
                    recorder.stop()
                    scope.cancel()
                    if(length < 10) {
                        lengthToAppend = "0$length"
                    } else {
                        lengthToAppend = "$length"
                    }
                    recording = false
                }
            } else if(event == Lifecycle.Event.ON_STOP) {
                if(pressedRec) {
                    file.delete()
                }
                navController.navigate(route = Screen.Home.route) {
                    popUpTo(Screen.Home.route) {
                        inclusive = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 30.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = recTextIndicator,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    fontWeight = MaterialTheme.typography.headlineLarge.fontWeight,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = (30-length).toString() + " s",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = MaterialTheme.typography.bodyLarge.fontWeight
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .height(144.dp)
                        .width(144.dp)
                        .clickable(
                            onClick = {
                                if (!pressedRec) {
                                    recorder.start(file)
                                    pressedRec = true
                                    scope.launch {
                                        while (length < 30) {
                                            delay(1000)
                                            length++
                                        }
                                    }
                                } else {
                                    recorder.stop()
                                    scope.cancel()
                                    if (length < 10) {
                                        lengthToAppend = "0$length"
                                    } else {
                                        lengthToAppend = "$length"
                                    }
                                }
                                recording = !recording
                            },
                            indication = null,
                            interactionSource = MutableInteractionSource()
                        )
                ) {
                    if(recording) {
                        AnimatedCircles()
                    } else {
                        DrawCircleOnCanvas(
                            scale = 3f,
                            color = MaterialTheme.colorScheme.primary,
                            radiusRatio = 12f
                        )
                    }

                }

                Spacer(modifier = Modifier.height(48.dp))

                Row {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if(reference != "" && reference != null) {
                                    File(folder, "$reference.mp3").delete()
                                    val newName = reference.dropLast(2)+lengthToAppend+"."+file.extension
                                    file.renameTo(File(folder, newName))
                                    navController.navigate(route = Screen.Detail.passRef(newName.dropLast(4)))
                                } else {
                                    val newName = fileName.dropLast(4)+lengthToAppend+"."+file.extension
                                    val newFile = File(folder, newName)
                                    if(!file.renameTo(newFile)) {
                                        Log.d("WARNING", "Il file NON è stato rinominato")
                                    }
                                    navController.navigate(route = Screen.Home.route) {
                                        popUpTo(Screen.Home.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            },
                            enabled = (pressedRec && !recording),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Done,
                                contentDescription = "Salva",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Salva",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            fontWeight = MaterialTheme.typography.bodyLarge.fontWeight
                        )
                    }

                    Spacer(modifier = Modifier.width(48.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                file.delete()
                                navController.navigate(route = Screen.Home.route) {
                                    popUpTo(Screen.Home.route) {
                                        inclusive = true
                                    }
                                }
                            },
                            enabled = (pressedRec && !recording),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if(isSystemInDarkTheme()) dark_DeleteContainer else light_DeleteContainer,
                                contentColor = if(isSystemInDarkTheme()) dark_onDeleteContainer else light_onDeleteContainer
                            ),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Elimina",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Elimina",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            fontWeight = MaterialTheme.typography.bodyLarge.fontWeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun RecScreenPreview() {
    MemoiceTheme {
        RecScreen(
            navController = rememberNavController(),
            recorder = AudioRecorder(LocalContext.current),
            folder = File("recs"),
            reference = ""
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
fun RecScreenDarkPreview() {
    MemoiceTheme {
        RecScreen(
            navController = rememberNavController(),
            recorder = AudioRecorder(LocalContext.current),
            folder = File("recs"),
            reference = ""
        )
    }
}