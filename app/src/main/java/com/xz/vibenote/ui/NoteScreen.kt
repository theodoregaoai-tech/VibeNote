package com.xz.vibenote.ui

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xz.vibenote.data.Note
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun getAudioDuration(path: String): Long {
    return try {
        if (!File(path).exists()) return 0L
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        retriever.release()
        duration
    } catch (_: Exception) {
        0L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel) {
    val dailyNotes by viewModel.dailyNotes.collectAsState()
    val currentText by viewModel.currentText.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search notes...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearSearch) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true
        )

        NoteList(
            dailyNotes = dailyNotes,
            isSearchActive = searchQuery.isNotEmpty(),
            onNoteClick = viewModel::selectNote,
            onEdit = viewModel::startEditing,
            onDelete = viewModel::deleteNote,
            modifier = Modifier.weight(1f)
        )
    }

    if (editingNote != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::cancelEditing,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Edit Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentText,
                    onValueChange = viewModel::onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Note content...") },
                    minLines = 3,
                    maxLines = 8
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = viewModel::cancelEditing) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = viewModel::saveNote,
                        enabled = currentText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (currentText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteList(
    dailyNotes: List<DailyNotes>,
    isSearchActive: Boolean,
    onNoteClick: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dailyNotes.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSearchActive) {
                Text(
                    "No notes matching search",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No notes yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Use the mic to record your first note",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp)
        ) {
            dailyNotes.forEach { group ->
                item {
                    DateHeader(group.dateLabel)
                }
                items(group.notes, key = { it.id }) { note ->
                    if (note.isAudioNote) {
                        AudioNoteCard(note = note, onClick = onNoteClick, onDelete = onDelete)
                    } else {
                        NoteCard(note = note, onClick = onNoteClick, onEdit = onEdit, onDelete = onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick(note) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = timeFormat.format(Date(note.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                IconButton(
                    onClick = { onEdit(note) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onDelete(note) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioNoteCard(
    note: Note,
    onClick: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val path = note.audioFilePath ?: return
    val fileExists = remember(path) { File(path).exists() }
    val durationMs = remember(path) { if (fileExists) getAudioDuration(path) else 0L }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(note.id) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick(note) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            if (!fileExists) return@IconButton
                            if (isPlaying) {
                                player?.pause()
                                isPlaying = false
                            } else {
                                if (player == null) {
                                    player = MediaPlayer().apply {
                                        setDataSource(path)
                                        prepare()
                                        setOnCompletionListener { isPlaying = false }
                                    }
                                }
                                player?.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (fileExists) "Audio Note · ${formatDuration(durationMs)}" else "Audio Note · File missing",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = timeFormat.format(Date(note.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                IconButton(
                    onClick = { onDelete(note) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
