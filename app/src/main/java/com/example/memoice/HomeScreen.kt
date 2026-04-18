package com.example.memoice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.memoice.navigation.Screen
import com.example.memoice.ui.theme.BagelFatOne
import com.example.memoice.ui.theme.dark_Delete
import com.example.memoice.ui.theme.light_Delete
import com.example.memoice.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val permission = Manifest.permission.RECORD_AUDIO
    
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

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Se l'utente accetta, la notifica comparirà. Altrimenti no.
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!isGranted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val records by viewModel.records.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadRecords()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(24.dp)
                ) { Text(data.visuals.message) }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text(
                    text = "Memoice",
                    fontFamily = BagelFatOne,
                    fontSize = 40.sp
                ) },
                scrollBehavior = scrollBehavior 
            )
        },

        floatingActionButtonPosition = FabPosition.Center,

        floatingActionButton = {
            LargeFloatingActionButton(
                content = {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Registra",
                        modifier = Modifier.size(48.dp)
                    )
                },

                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,

                elevation = FloatingActionButtonDefaults.elevation(1.dp),

                shape = MaterialShapes.Cookie6Sided.toShape(),

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
            color = MaterialTheme.colorScheme.surfaceContainer,
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    )
                ) {
                    itemsIndexed(items = records) { index, record ->
                        var showMenu by remember { mutableStateOf(false) }

                        SegmentedListItem(
                            onClick = {
                                navController.navigate(route = Screen.Detail.passRef(record.file.nameWithoutExtension))
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index = index,
                                count = records.size
                            ),
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface, // o MaterialTheme.colorScheme.surfaceContainer
                            ),
                            leadingContent = {
                                Text(
                                    text = "${index+1}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            },
                            trailingContent = {
                                // Usiamo un Row per combinare il testo della durata e il menu nel lato destro
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = record.durationStr,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 4.dp) // Leggero margine per separare dal menu
                                    )

                                    IconButton(onClick = { showMenu = !showMenu }) {
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
                                                    showMenu = false
                                                    navController.navigate(route = Screen.Detail.passRef(record.file.nameWithoutExtension))
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
                        ) {
                            Text(
                                text = record.file.nameWithoutExtension,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                lineHeight = 22.sp
                            )
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