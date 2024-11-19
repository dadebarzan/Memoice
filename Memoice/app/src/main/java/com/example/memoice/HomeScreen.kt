package com.example.memoice

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.memoice.navigation.Screen
import com.example.memoice.ui.theme.MemoiceTheme
import com.example.memoice.ui.theme.dark_Delete
import com.example.memoice.ui.theme.light_Delete
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    folder: File
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val permission = android.Manifest.permission.RECORD_AUDIO
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(isGranted) {
            navController.navigate(route = Screen.Rec.route)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Accesso al microfono necessario"
                )
            }
        }
    }

    val records = remember { mutableStateListOf<File>(*folder.listFiles()) }
    var update by remember { mutableStateOf(false) }
    var candidate: File? by remember { mutableStateOf(null) }

    if(update) {
        records.remove(candidate)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                data -> Snackbar(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(24.dp)
            ) { Text(data.visuals.message) }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(text = "Memoice", fontSize = 34.sp)
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(
                        text = "Registra"
                    )
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.graphic_eq),
                        contentDescription = "Registra"
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                expanded = true,
                onClick = {
                    checkAndRequestAudioRecPermission(context, permission, launcher, navController)
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
            if(records.isEmpty())
                Text(
                    text = "Registra una nuova\nnota vocale con il pulsante\n\"Registra\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(top = 48.dp)
                )
            else {
                LazyColumn {
                    itemsIndexed(items = records) { index, record ->
                        var showMenu by remember{ mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index+1}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(0.7f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = record.nameWithoutExtension.dropLast(2),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .weight(4f)
                                    .clickable(
                                        onClick = {
                                            navController.navigate(Screen.Detail.passRef(record.nameWithoutExtension))
                                        }
                                    )
                            )
                            Text(
                                text = "00:"+record.nameWithoutExtension.drop(record.nameWithoutExtension.length-2),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    showMenu = !showMenu
                                },
                                modifier = Modifier.weight(0.5f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Opzioni",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Dettagli",
                                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                                fontWeight = MaterialTheme.typography.labelLarge.fontWeight
                                            )
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        onClick = {
                                            navController.navigate(route = Screen.Detail.passRef(record.nameWithoutExtension))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Elimina",
                                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                                fontWeight = MaterialTheme.typography.labelLarge.fontWeight
                                            )
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = if(isSystemInDarkTheme()) dark_Delete else light_Delete
                                        ),
                                        onClick = {
                                            update = true
                                            candidate = record
                                            record.delete()
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun HomeScreenPreview() {
    MemoiceTheme {
        HomeScreen(
            navController = rememberNavController(),
            folder = File("recs")
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
fun HomeScreenDarkPreview() {
    MemoiceTheme {
        HomeScreen(
            navController = rememberNavController(),
            folder = File("recs")
        )
    }
}

fun checkAndRequestAudioRecPermission(
    context: Context,
    permission: String,
    launcher: ManagedActivityResultLauncher<String, Boolean>,
    navController: NavController
) {
    val permissionCheckResult = ContextCompat.checkSelfPermission(context, permission)
    if(permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
        navController.navigate(Screen.Rec.route)
    } else {
        launcher.launch(permission)
    }
}


/*
@Composable
fun RecordItem(
    index: Int,
    file: File,
    navController: NavController
) {
    var showMenu by remember{ mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index+1}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp,
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.Center
        )
        Text(
            text = file.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            modifier = Modifier
                .weight(4f)
                .clickable(
                    onClick = {
                        navController.navigate(Screen.Detail.passRef(file.nameWithoutExtension))
                    }
                )
        )
        /*Text(
            text = if(record.length < 10) "00:0${record.length}" else "00:${record.length}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )*/
        IconButton(
            onClick = {
                showMenu = !showMenu
            },
            modifier = Modifier.weight(1f) //0.5f
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Opzioni",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Dettagli",
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            fontWeight = MaterialTheme.typography.labelLarge.fontWeight
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = {
                        navController.navigate(route = Screen.Detail.passRef(file.nameWithoutExtension))
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Elimina",
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            fontWeight = MaterialTheme.typography.labelLarge.fontWeight
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = if(isSystemInDarkTheme()) dark_Delete else light_Delete
                    ),
                    onClick = {
                        file.delete()
                        showMenu = false

                    }
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun RecordItemPreview() {
    MemoiceTheme {
        Surface {
            RecordItem(index = 13, file = example, navController = rememberNavController())
        }
    }
}

@Composable
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
fun RecordItemDarkPreview() {
    MemoiceTheme {
        Surface {
            RecordItem(index = 13, file = example, navController = rememberNavController())
        }
    }
}

val example = File("File_esempio12.mp3")
*/