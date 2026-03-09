package com.example.memoice

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.memoice.navigation.LockScreenOrientation
import com.example.memoice.navigation.Screen
import com.example.memoice.viewmodel.DetailViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel,
    folder: File,
    reference: String // Ora reference è IL NOME ESATTO del file (senza .mp3)
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val scope = rememberCoroutineScope()
    val file = File(folder, "$reference.mp3")

    // Dati recuperati dal ViewModel
    val durationSeconds = remember { viewModel.getDuration(file) }
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val (date, time) = remember { viewModel.getFileDateInfo(file) }

    // Stati osservati dal ViewModel
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    // Sincronizza il pallino con l'audio reale SOLO SE l'utente non lo sta toccando
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
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
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (rename) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                if (!newText.contains(' ') && !newText.contains('/') && newText.length < 20) {
                                    text = newText
                                }
                            },
                            label = { Text("Titolo") },
                            maxLines = 1,
                            modifier = Modifier.padding(start = 12.dp).weight(1f)
                        )
                    } else {
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(12.dp).weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    FilledTonalIconButton(
                        onClick = {
                            if (rename && text.isNotBlank() && text != reference) {
                                scope.launch {
                                    viewModel.renameFile(file, text)
                                    // Aggiorniamo la UI ricaricando la pagina con il nuovo riferimento
                                    navController.navigate(Screen.Detail.passRef(text)) {
                                        popUpTo(Screen.Home.route)
                                    }
                                }
                            }
                            rename = !rename
                        },
                        enabled = !isPlaying,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (rename) Icons.Outlined.Done else Icons.Outlined.Edit,
                            contentDescription = if (rename) "Salva" else "Modifica"
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    IconButton(
                        onClick = {
                            if (isPlaying) viewModel.pauseAudio() else viewModel.playAudio(file)
                        }
                    ) {
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
                    }
                    
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
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    
                    Text(text = formattedDuration, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    text = "Registrato il $date alle $time",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp)
                )

                Row(modifier = Modifier.padding(top = 36.dp).align(Alignment.CenterHorizontally)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalIconButton(
                            onClick = {
                                viewModel.stopAudio()
                                navController.navigate(Screen.Rec.passRef(reference))
                            },
                            enabled = !isPlaying,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Registra di nuovo", modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "Registra\ndi nuovo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}