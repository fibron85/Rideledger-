package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.util.LocalStorageManager
import com.example.util.StorageEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StorageSetupDialog(
    appDataFolderPath: String,
    onGrantFolderPermission: () -> Unit,
    onUseAppDataFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onUseAppDataFolder,
        icon = {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "Local Storage Setup",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Local Data Storage & Folder Access") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "RideLedger has created a dedicated local 'data' folder inside its own app folder to save your rides automatically:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = appDataFolderPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Text(
                    text = "Grant Read & Write permission to a shared folder (such as Downloads or Documents) so you can easily export reports and browse Excel backups for import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGrantFolderPermission,
                modifier = Modifier.testTag("storage_setup_grant_button")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Grant Folder Access")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onUseAppDataFolder,
                modifier = Modifier.testTag("storage_setup_use_app_folder_button")
            ) {
                Text("Use App Folder Only")
            }
        }
    )
}

@Composable
fun ExportDestinationDialog(
    fileName: String,
    formatLabel: String,
    grantedFolderUri: String?,
    onDismiss: () -> Unit,
    onSaveToGrantedFolder: () -> Unit,
    onChooseDestinationFolder: () -> Unit,
    onSaveToAppDataFolder: () -> Unit,
    onUseSystemSavePicker: () -> Unit
) {
    val context = LocalContext.current
    val hasWritePermission = remember(grantedFolderUri) {
        LocalStorageManager.hasWriteTreePermission(context, grantedFolderUri)
    }
    val folderDisplayName = remember(grantedFolderUri) {
        if (hasWritePermission) LocalStorageManager.getTreeFolderDisplayName(context, grantedFolderUri)
        else null
    }
    val appDataPath = remember { LocalStorageManager.getAppDataFolderDisplayPath(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Export Destination",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Export $formatLabel Destination") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (hasWritePermission) {
                        "Choose a destination folder or save directly inside your granted folder."
                    } else {
                        "Please grant destination folder access & permission to save your exported file there."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasWritePermission && folderDisplayName != null) {
                    Button(
                        onClick = onSaveToGrantedFolder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_to_granted_folder_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to \"$folderDisplayName\"")
                    }
                }

                FilledTonalButton(
                    onClick = onChooseDestinationFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_pick_folder_button")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasWritePermission) "Change Destination Folder" else "Choose Destination Folder & Grant Permission")
                }

                OutlinedButton(
                    onClick = onSaveToAppDataFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_to_app_data_button")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save in App 'data' Folder")
                }

                Text(
                    text = "App folder: $appDataPath",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onUseSystemSavePicker,
                modifier = Modifier.testTag("export_system_picker_button")
            ) {
                Text("System Save As")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportPermissionDialog(
    onDismiss: () -> Unit,
    onGrantFolderPermission: () -> Unit,
    onBrowseAppDataFolder: () -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Folder Permission Required",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Folder & File Permission Needed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "To browse and import Excel (.xlsx) backups from device storage, grant folder access permission. Once granted, the file browser will open automatically.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onGrantFolderPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_grant_permission_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grant Folder Access & Browse")
                }
                FilledTonalButton(
                    onClick = onBrowseAppDataFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_browse_app_data_button")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse App 'data' Folder")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onOpenSystemPicker,
                modifier = Modifier.testTag("import_system_file_picker_button")
            ) {
                Text("System File Picker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportFileBrowserDialog(
    grantedFolderUri: String?,
    initialUseAppFolder: Boolean = false,
    onDismiss: () -> Unit,
    onSelectEntry: (StorageEntry) -> Unit,
    onChangeFolderPermission: () -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val hasGrantedTree = remember(grantedFolderUri) {
        LocalStorageManager.hasValidTreePermission(context, grantedFolderUri)
    }
    val grantedFolderName = remember(grantedFolderUri, hasGrantedTree) {
        if (hasGrantedTree) LocalStorageManager.getTreeFolderDisplayName(context, grantedFolderUri) else "Granted Folder"
    }

    var useAppFolder by remember(hasGrantedTree, initialUseAppFolder) {
        mutableStateOf(initialUseAppFolder || !hasGrantedTree)
    }

    val treeDocIdStack = remember { mutableStateListOf<Pair<String?, String>>() }
    var currentLocalDir by remember {
        mutableStateOf(LocalStorageManager.ensureAppDataFolder(context))
    }
    var entries by remember { mutableStateOf<List<StorageEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val currentTreeDocId = treeDocIdStack.lastOrNull()?.first
    val currentBreadcrumb = if (useAppFolder) {
        currentLocalDir.name
    } else {
        treeDocIdStack.lastOrNull()?.second ?: grantedFolderName
    }

    LaunchedEffect(useAppFolder, grantedFolderUri, currentTreeDocId, currentLocalDir) {
        isLoading = true
        loadError = null
        if (useAppFolder) {
            val list = withContext(Dispatchers.IO) {
                LocalStorageManager.listLocalFolderChildren(currentLocalDir)
            }
            entries = list
            isLoading = false
        } else if (!grantedFolderUri.isNullOrBlank()) {
            val result = withContext(Dispatchers.IO) {
                LocalStorageManager.listTreeChildren(context, grantedFolderUri, currentTreeDocId)
            }
            result.fold(
                onSuccess = { entries = it },
                onFailure = { loadError = it.message ?: "Unable to read folder contents." }
            )
            isLoading = false
        } else {
            entries = emptyList()
            isLoading = false
        }
    }

    val appRootDir = remember { LocalStorageManager.ensureAppDataFolder(context) }
    val canGoUp = if (useAppFolder) {
        currentLocalDir.absolutePath != appRootDir.absolutePath && currentLocalDir.parentFile != null
    } else {
        treeDocIdStack.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Excel Backup Browser") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasGrantedTree) {
                        FilterChip(
                            selected = !useAppFolder,
                            onClick = {
                                useAppFolder = false
                                treeDocIdStack.clear()
                            },
                            label = {
                                Text(
                                    text = grantedFolderName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                    FilterChip(
                        selected = useAppFolder,
                        onClick = {
                            useAppFolder = true
                            currentLocalDir = appRootDir
                        },
                        label = { Text("App 'data' Folder") }
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canGoUp) {
                            IconButton(
                                onClick = {
                                    if (useAppFolder) {
                                        currentLocalDir.parentFile?.let { parent: File ->
                                            currentLocalDir = parent
                                        }
                                    } else if (treeDocIdStack.isNotEmpty()) {
                                        treeDocIdStack.removeAt(treeDocIdStack.lastIndex)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Parent folder",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = currentBreadcrumb,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        loadError != null -> {
                            Text(
                                text = loadError ?: "Unable to load files.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        entries.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No .xlsx backup files found in this folder.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Export or save an Excel backup first, change folder, or tap Browse All Files.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(entries, key = { "${it.isDirectory}_${it.name}_${it.documentId ?: it.localFile?.absolutePath}" }) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (entry.isDirectory) {
                                                    if (useAppFolder) {
                                                        entry.localFile?.let { currentLocalDir = it }
                                                    } else {
                                                        treeDocIdStack.add(entry.documentId to entry.name)
                                                    }
                                                } else {
                                                    onSelectEntry(entry)
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.TableView,
                                            contentDescription = if (entry.isDirectory) "Folder" else "Excel file",
                                            tint = if (entry.isDirectory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!entry.isDirectory) {
                                                Text(
                                                    text = LocalStorageManager.formatFileSize(entry.sizeBytes),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onChangeFolderPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change Folder", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenSystemPicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("System Picker", maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
