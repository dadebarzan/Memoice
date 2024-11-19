package com.example.memoice

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.memoice.navigation.LockScreenOrientation
import com.example.memoice.navigation.Screen
import com.example.memoice.service.AudioPlayerViewModel
import com.example.memoice.ui.theme.MemoiceTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavController,
    serviceViewModel: AudioPlayerViewModel,
    folder: File,
    reference: String
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { Manifest.permission.POST_NOTIFICATIONS } else { Manifest.permission.RECORD_AUDIO }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val permissionCheckResult = ContextCompat.checkSelfPermission(context, permission)

    val service = serviceViewModel.getMyService()

    val file = File(folder, "$reference.mp3")
    val length = reference.drop(reference.length-2).toInt()
    val lengthS = if(length < 10) "0$length" else "$length"
    var stringTime = "1970-01-01T00:00:00Z"
    try {
        stringTime = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).creationTime().toString()
    } catch(e: NoSuchFileException) {
        navController.navigate(route = Screen.Home.route) {
            popUpTo(Screen.Home.route) {
                inclusive = true
            }
        }
    }
    if(stringTime == "1970-01-01T00:00:00Z") {
        navController.navigate(route = Screen.Home.route) {
            popUpTo(Screen.Home.route) {
                inclusive = true
            }
        }
    }
    val dateAndTime = LocalDateTime.parse(stringTime, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'kk:mm:ssX"))
    val date = dateAndTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd LLLL uuuu")).toString()
    val time = dateAndTime.toLocalTime().format(DateTimeFormatter.ofPattern("kk:mm")).toString()

    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var rename by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(file.nameWithoutExtension.dropLast(2)) }

    if(progress == 1f) {
        scope.cancel()
        progress = 0f
        playing = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(text = "Dettagli", fontSize = 34.sp)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate(route = Screen.Home.route) {
                                popUpTo(Screen.Home.route) {
                                    inclusive = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Home"
                        )
                    }
                }
            )
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(
                    modifier = Modifier.height(24.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if(rename) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                if(!newText.contains(' ') && !newText.contains('/') && newText.length < 12) {
                                    text = newText
                                }
                            },
                            label = { Text( text = "Titolo" ) },
                            maxLines = 1,
                            colors = TextFieldDefaults.colors(
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    } else {
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 40.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if(rename && inputCheck(text, folder)) {
                                val newName = text+lengthS+"."+file.extension
                                try {
                                    file.renameTo(File(folder, newName))
                                } catch(e: NoSuchFileException) {
                                    navController.navigate(route = Screen.Detail.passRef(newName.dropLast(4))) {
                                        popUpTo(Screen.Home.route)
                                    }
                                }
                            }
                            rename = !rename
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        enabled = !playing,
                        modifier = Modifier.offset(y = 5.dp)
                    ) {
                        if(rename) {
                            Icon(
                                imageVector = Icons.Outlined.Done,
                                contentDescription = "Salva"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Modifica"
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if(playing) {
                                service?.stop()
                                scope.cancel()
                                progress = 0f
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (permissionCheckResult != PackageManager.PERMISSION_GRANTED) {
                                        launcher.launch(permission)
                                    }
                                }
                                service?.play(file.toUri(), file.name)
                                scope.launch {
                                    while(progress < 1f) {
                                        if (service?.isPlaying() == true) {
                                            delay(100)
                                            playing = true
                                            progress = service.getCurrentPosition()
                                        }
                                    }
                                }
                            }
                            playing = !playing
                        }
                    ) {
                        if(playing) {
                            Icon(
                                painter = painterResource(id = R.drawable.pause),
                                contentDescription = "Metti in pausa"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Riproduci"
                            )
                        }
                    }
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                        progress = progress
                    /* TODO: seekTo e scorrimento */
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "00:$lengthS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Registrato il $date alle $time",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 12.dp, start = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                navController.navigate(Screen.Rec.passRef(file.nameWithoutExtension))
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            enabled = !playing,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Registra di nuovo",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Registra\ndi nuovo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            fontWeight = MaterialTheme.typography.bodyLarge.fontWeight,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private fun inputCheck(title: String, folder: File): Boolean {
    if(title.contains(' ') || title.contains('/')) {
        return false
    } else if(title.length > 11) {
        return false
    } else {
        val files = folder.listFiles()
        for(file in files!!) {
            if(title == file.nameWithoutExtension) {
                return false
            }
        }
        return true
    }
}

@Composable
@Preview(showBackground = true)
fun DetailScreenPreview() {
    MemoiceTheme {
        DetailScreen(
            navController = rememberNavController(),
            serviceViewModel = AudioPlayerViewModel(),
            folder = File("recs"),
            reference = "Esempio12.mp3"
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
fun DetailScreenDarkPreview() {
    MemoiceTheme {
        DetailScreen(
            navController = rememberNavController(),
            serviceViewModel = AudioPlayerViewModel(),
            folder = File("recs"),
            reference = "Esempio12.mp3"
        )
    }
}

