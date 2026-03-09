package com.example.memoice

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.memoice.ui.theme.*
import com.example.memoice.viewmodel.RecordViewModel

@Composable
fun RecScreen(
    navController: NavController,
    viewModel: RecordViewModel, // Usiamo il ViewModel!
    reference: String?
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val isRecording by viewModel.isRecording.collectAsState()
    val length by viewModel.recordDuration.collectAsState()
    
    // Lo stato del bottone (se è stato premuto almeno una volta)
    var pressedRec by remember { mutableStateOf(false) }

    val recTextIndicator = when {
        pressedRec && isRecording -> "Registrazione\nin corso..."
        pressedRec && !isRecording -> "Registrazione\ninterrotta!"
        else -> "Premi per\nregistrare"
    }

    // Creiamo il riferimento al file solo una volta all'avvio della schermata
    val outputFile = remember { viewModel.getOutputFile(reference) }

    // Gestione del ciclo di vita (es. se l'utente mette l'app in background)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onPause()
            } else if (event == Lifecycle.Event.ON_STOP && pressedRec && isRecording) {
                viewModel.cancelRecording()
                navController.popBackStack(Screen.Home.route, false)
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
                    text = "${30 - length} s",
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
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isRecording && !pressedRec) {
                                viewModel.startRecording(outputFile)
                                pressedRec = true
                            } else if (isRecording) {
                                viewModel.stopRecording()
                            }
                        }
                ) {
                    if (isRecording) {
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
                    // Pulsante Salva
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalIconButton(
                            onClick = {
                                if (reference != "" && reference != null) {
                                    // Se stavamo sovrascrivendo, torniamo al dettaglio
                                    navController.navigate(route = Screen.Detail.passRef(outputFile.nameWithoutExtension)) {
                                        popUpTo(Screen.Home.route)
                                    }
                                } else {
                                    // Altrimenti torniamo alla Home
                                    navController.popBackStack(Screen.Home.route, false)
                                }
                            },
                            enabled = (pressedRec && !isRecording),
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

                    // Pulsante Elimina (Annulla)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalIconButton(
                            onClick = {
                                viewModel.cancelRecording()
                                navController.popBackStack(Screen.Home.route, false)
                            },
                            enabled = (pressedRec && !isRecording),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isSystemInDarkTheme()) dark_DeleteContainer else light_DeleteContainer,
                                contentColor = if (isSystemInDarkTheme()) dark_onDeleteContainer else light_onDeleteContainer
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