package com.foodpack.downtime.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodpack.downtime.data.DowntimeRepository
import com.foodpack.downtime.sync.NoteFile
import com.foodpack.downtime.sync.NotesUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ACCEPTED_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.oasis.opendocument.text",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.oasis.opendocument.spreadsheet"
)

/**
 * One Notes subfolder: what's already attached, and a button to add a
 * new file. Uploading doesn't need a Google sign-in — the file is sent
 * to the same Apps Script backend the downtime log already uses, which
 * drops it into the matching Drive folder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFolderScreen(folder: String, repository: DowntimeRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webhookUrl by repository.settings.webhookUrl.collectAsState(initial = "")

    var files by remember { mutableStateOf<List<NoteFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            errorMessage = null
            val uploader = NotesUploader(webhookUrl)
            val result = withContext(Dispatchers.IO) { uploader.listFiles(folder) }
            result.onSuccess { files = it }
                .onFailure { errorMessage = it.message ?: "Couldn't load files" }
            loading = false
        }
    }

    LaunchedEffect(webhookUrl, folder) {
        if (webhookUrl.isNotBlank()) refresh()
        else loading = false
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            errorMessage = null
            try {
                val (name, mimeType, bytes) = withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val displayName = resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                    } ?: "file"
                    val type = resolver.getType(uri) ?: "application/octet-stream"
                    val data = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    Triple(displayName, type, data)
                }
                val uploader = NotesUploader(webhookUrl)
                val result = withContext(Dispatchers.IO) {
                    uploader.uploadFile(folder, name, mimeType, bytes)
                }
                result.onFailure { errorMessage = it.message ?: "Upload failed" }
                refresh()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Upload failed"
            } finally {
                uploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp)) {
            if (webhookUrl.isBlank()) {
                Text(
                    "Set up the Google Sheet link in Settings first — Notes uses the same connection.",
                    color = MaterialTheme.colorScheme.error
                )
                return@Column
            }

            Button(
                onClick = { pickFileLauncher.launch(ACCEPTED_MIME_TYPES) },
                enabled = !uploading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (uploading) "Uploading…" else "📎 Attach a file", fontWeight = FontWeight.Bold)
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                files.isEmpty() -> {
                    Text("No files here yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(files) { file ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.url))
                                                context.startActivity(intent)
                                            } catch (e: ActivityNotFoundException) {
                                                errorMessage = "No app found to open this file. Try opening it in Drive instead."
                                            }
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Updated ${file.updatedAt.take(10)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}
