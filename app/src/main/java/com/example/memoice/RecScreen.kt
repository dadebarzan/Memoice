package com.example.memoice

import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.memoice.navigation.LockScreenOrientation
import com.example.memoice.navigation.Screen
import com.example.memoice.ui.theme.AnimatedCircles
import com.example.memoice.ui.theme.DrawCircleOnCanvas
import com.example.memoice.ui.theme.dark_DeleteContainer
import com.example.memoice.ui.theme.dark_onDeleteContainer
import com.example.memoice.ui.theme.light_DeleteContainer
import com.example.memoice.ui.theme.light_onDeleteContainer
import com.example.memoice.viewmodel.RecordViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecScreen(
    navController: NavController,
    viewModel: RecordViewModel
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val isRecording by viewModel.isRecording.collectAsState()
    val length by viewModel.recordDuration.collectAsState()
    val minutes = length / 60
    val seconds = length % 60
    val formattedTime = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    
    var pressedRec by remember { mutableStateOf(false) }

    val recTextIndicator = when {
        pressedRec && isRecording -> "Registrazione\nin corso..."
        pressedRec && !isRecording -> "Registrazione\nconclusa!"
        else -> "Premi per\nregistrare"
    }

    // Creiamo il riferimento al file solo una volta all'avvio della schermata
    val outputFile = remember { viewModel.getOutputFile() }

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
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 48.dp, top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = recTextIndicator,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.headlineLargeEmphasized.fontSize,
                fontWeight = MaterialTheme.typography.headlineLargeEmphasized.fontWeight,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(modifier = Modifier.weight(0.4f))
            Text(
                text = formattedTime,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = MaterialTheme.typography.titleMedium.fontWeight
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
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
            Spacer(modifier = Modifier.weight(1f))
            ButtonGroup(
                modifier = Modifier
                    .padding(horizontal = 32.dp),
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(
                        menuState,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                        modifier = Modifier.size(IconButtonDefaults.extraLargeIconSize)
                    )
                }
            ) {
                customItem(
                    {
                        FilledTonalButton(
                            onClick = {
                                viewModel.stopRecording()
                                navController.popBackStack(Screen.Home.route, false)
                            },
                            enabled = (pressedRec && !isRecording),
                            modifier = Modifier
                                .height(136.dp)
                                .weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = IconButtonDefaults.extraLargeRoundShape
                        ) {
                            Icon(
                                Icons.Outlined.Done,
                                contentDescription = "Salva",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    },
                    menuContent = {
                        DropdownMenuItem(
                            leadingIcon = { Icon(
                                Icons.Outlined.Done,
                                contentDescription = "Salva"
                            ) },
                            text = { Text("Salva") },
                            onClick = {
                                viewModel.stopRecording()
                                navController.popBackStack(Screen.Home.route, false)
                            }
                        )
                    }
                )
                customItem(
                    {
                        FilledTonalButton(
                            onClick = {
                                viewModel.cancelRecording()
                                navController.popBackStack(Screen.Home.route, false)
                            },
                            enabled = (!isRecording),
                            modifier = Modifier
                                .height(136.dp)
                                .weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSystemInDarkTheme()) dark_DeleteContainer else light_DeleteContainer,
                                contentColor = if (isSystemInDarkTheme()) dark_onDeleteContainer else light_onDeleteContainer
                            ),
                            shape = IconButtonDefaults.extraLargeSquareShape
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Annulla registrazione",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    },
                    menuContent = {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Annulla registrazione"
                                )
                            },
                            text = { Text("Annulla registrazione") },
                            onClick = {
                                viewModel.cancelRecording()
                                navController.popBackStack(Screen.Home.route, false)
                            }
                        )
                    }
                )
            }
        }
    }
}