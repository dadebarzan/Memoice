package com.example.memoice

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.memoice.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel // <-- Ora passiamo il ViewModel invece del File folder
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

    // Osserviamo la lista di record dal ViewModel. Se cambia, la UI si aggiorna da sola!
    val records by viewModel.records.collectAsState()

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
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
                title = { Text(text = "Memoice", fontSize = 34.sp) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "Registra") },
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
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            if(records.isEmpty()) {
                Text(
                    text = "Registra una nuova\nnota vocale con il pulsante\n\"Registra\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(top = 48.dp)
                )
            } else {
                LazyColumn {
                    itemsIndexed(items = records) { index, record ->
                        var showMenu by remember { mutableStateOf(false) }

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
                                text = record.nameWithoutExtension, // <-- Semplificato, niente dropLast!
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .weight(4f)
                                    .clickable {
                                        navController.navigate(Screen.Detail.passRef(record.nameWithoutExtension))
                                    }
                            )
                            // La durata non è più calcolata dal nome. Per ora mettiamo un placeholder,
                            // poi la leggeremo dal ViewModel in modo pulito.
                            Text(
                                text = "Audio", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showMenu = !showMenu },
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
                                        text = { Text("Dettagli") },
                                        onClick = {
                                            navController.navigate(route = Screen.Detail.passRef(record.nameWithoutExtension))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Elimina") },
                                        colors = MenuDefaults.itemColors(
                                            textColor = if(isSystemInDarkTheme()) dark_Delete else light_Delete
                                        ),
                                        onClick = {
                                            viewModel.deleteRecord(record)
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