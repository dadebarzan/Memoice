package com.example.memoice

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.memoice.navigation.LockScreenOrientation
import com.example.memoice.navigation.Screen
import com.example.memoice.ui.theme.dark_DeleteContainer
import com.example.memoice.ui.theme.dark_onDeleteContainer
import com.example.memoice.ui.theme.light_DeleteContainer
import com.example.memoice.ui.theme.light_onDeleteContainer
import com.example.memoice.viewmodel.DetailViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel,
    folder: File,
    reference: String
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val file = remember(reference, folder) {
        val directFile = File(folder, reference)
        if (directFile.exists() && directFile.isFile) {
            directFile
        } else {
            folder.listFiles()?.firstOrNull { it.nameWithoutExtension == reference }
                // Fallback di sicurezza
                ?: File(folder, "$reference.m4a")
        }
    }

    // Dati recuperati dal ViewModel
    LaunchedEffect(file) {
        viewModel.loadFileInfo(file)
    }

    // Osserviamo i dati asincroni
    val durationSeconds by viewModel.durationSeconds.collectAsState()
    val fileDateInfo by viewModel.fileDateInfo.collectAsState()
    val (date, time) = fileDateInfo

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    // Stati osservati dal ViewModel
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    // Deriviamo il tempo riprodotto dalla posizione dello slider
    val currentSecondsTotal = (sliderValue * durationSeconds).toInt()
    val currMinutes = currentSecondsTotal / 60
    val currSeconds = currentSecondsTotal % 60
    val formattedCurrentTime = "${currMinutes.toString().padStart(2, '0')}:${currSeconds.toString().padStart(2, '0')}"

    LaunchedEffect(progress) {
        if (!isDragging) {
            sliderValue = progress
        }
    }

    var rename by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(reference) }

    if (progress == 1f) {
        viewModel.resetProgress()
    }

    BackHandler {
        viewModel.stopAudio()
        navController.popBackStack()
    }

    val focusManager = LocalFocusManager.current
    val performSave = {
        val cleanText = text.trim()
        if (cleanText.isNotBlank() && cleanText != reference) {
            scope.launch {
                val success = viewModel.renameFile(file, cleanText)
                if (success) {
                    navController.navigate(Screen.Detail.passRef(cleanText)) {
                        popUpTo(Screen.Home.route)
                    }
                } else {
                    text = reference
                    snackbarHostState.showSnackbar("Nome non valido o già esistente")
                }
            }
        }
        rename = false
        focusManager.clearFocus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(text = "Dettagli", fontSize = 34.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.stopAudio()
                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(all = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (rename) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                if (!newText.contains('/') && newText.length < 32) {
                                    text = newText
                                }
                            },
                            label = { Text("Titolo") },
                            maxLines = 1,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { performSave() }
                            ),
                            modifier = Modifier.padding(12.dp).weight(1f)
                        )
                    } else {
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            modifier = Modifier.padding(12.dp).weight(1f)
                        )
                    }
                }

                Text(
                    text = "Registrato il $date alle $time",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp)
                )

                Spacer(modifier = Modifier.weight(2f))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LinearWavyProgressIndicator(
                            progress = { sliderValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        Slider(
                            value = sliderValue,
                            onValueChange = { newValue ->
                                isDragging = true
                                sliderValue = newValue
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                viewModel.seekTo(sliderValue)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                activeTrackColor = androidx.compose.ui.graphics.Color.Transparent,
                                inactiveTrackColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            thumb = {
                                Spacer(
                                    modifier = Modifier
                                        .size(width = 4.dp, height = 32.dp)
                                        .offset(x = 1.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedCurrentTime,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = " | " + formattedDuration,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(0.8f))

                /*Row(modifier = Modifier.padding(top = 36.dp).align(Alignment.CenterHorizontally)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalIconButton(
                            onClick = {
                                viewModel.stopAudio()
                                viewModel.deleteFile(file) {
                                    navController.popBackStack()
                                }
                            },
                            enabled = !isPlaying,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Elimina", modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "Elimina",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }*/

                ButtonGroup(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(
                            menuState,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(),
                            modifier = Modifier.size(IconButtonDefaults.largeIconSize)
                        )
                    }
                ) {
                    customItem(
                        {
                            FilledTonalButton(
                                onClick = {
                                    if (isPlaying) viewModel.pauseAudio() else viewModel.playAudio(file)
                                },
                                modifier = Modifier
                                    .height(96.dp)
                                    .weight(2f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape =
                                    if (isPlaying)
                                        IconButtonDefaults.largePressedShape
                                    else
                                        IconButtonDefaults.largeRoundShape
                            ) {
                                if (isPlaying) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.pause),
                                        contentDescription = "Pausa",
                                        modifier = Modifier.size(48.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Riproduci",
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                leadingIcon = {
                                    if (isPlaying) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.pause),
                                            contentDescription = "Pausa"
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Riproduci"
                                        )
                                    }
                                },
                                text = {
                                    if (isPlaying) {
                                        Text("Pausa")
                                    } else {
                                        Text("Riproduci")
                                    }},
                                onClick = {
                                    if (isPlaying) viewModel.pauseAudio() else viewModel.playAudio(file)
                                }
                            )
                        }
                    )

                    customItem(
                        {
                            FilledTonalButton(
                                onClick = {
                                    if (rename) {
                                        performSave()
                                    } else {
                                        rename = true
                                    }
                                },
                                enabled = (!isPlaying),
                                modifier = Modifier
                                    .height(96.dp)
                                    .weight(0.8f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = if (rename) IconButtonDefaults.largeSelectedRoundShape else IconButtonDefaults.largeRoundShape
                            ) {
                                Icon(
                                    imageVector = if (rename) Icons.Outlined.Done else Icons.Outlined.Edit,
                                    contentDescription = if (rename) "Salva" else "Modifica",
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (rename) Icons.Outlined.Done else Icons.Outlined.Edit,
                                        contentDescription = if (rename) "Salva" else "Modifica"
                                    )
                                },
                                text = { if (rename) Text("Salva") else Text("Modifica") },
                                onClick = {
                                    if (rename) {
                                        performSave()
                                    } else {
                                        rename = true
                                    }
                                }
                            )
                        }
                    )

                    customItem(
                        {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.stopAudio()
                                    viewModel.deleteFile(file) {
                                        navController.popBackStack()
                                    }
                                },
                                enabled = (!isPlaying),
                                modifier = Modifier
                                    .height(96.dp)
                                    .weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSystemInDarkTheme()) dark_DeleteContainer else light_DeleteContainer,
                                    contentColor = if (isSystemInDarkTheme()) dark_onDeleteContainer else light_onDeleteContainer
                                ),
                                shape = IconButtonDefaults.largeSquareShape
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Elimina",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Elimina"
                                    )
                                },
                                text = { Text("Elimina") },
                                onClick = {
                                    viewModel.stopAudio()
                                    viewModel.deleteFile(file) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}