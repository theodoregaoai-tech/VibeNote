package com.xz.vibenote.ui

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xz.vibenote.data.Note
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(note: Note, viewModel: NoteViewModel) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    BackHandler { viewModel.clearSelection() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (note.isAudioNote) "Audio Note" else "Note",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!note.isAudioNote) {
                        IconButton(onClick = { viewModel.startEditing(note) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit note",
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.deleteNote(note) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = dateFormat.format(Date(note.timestamp)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeFormat.format(Date(note.timestamp)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (note.isAudioNote) {
                AudioPlayerContent(note)
            } else {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 27.sp,
                )
            }
        }
    }
}

@Composable
private fun AudioPlayerContent(note: Note) {
    val path = note.audioFilePath ?: return
    val fileExists = remember(path) { File(path).exists() }

    if (!fileExists) {
        Text(
            text = "Audio file not found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }

    val durationMs = remember(path) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            d
        } catch (_: Exception) { 0L }
    }

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    DisposableEffect(note.id) {
        onDispose {
            player?.release()
            player = null
        }
    }

    // Update slider position while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(250)
            if (!isSeeking) {
                player?.let { mp ->
                    val dur = mp.duration.toFloat()
                    if (dur > 0) {
                        sliderPosition = mp.currentPosition / dur
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = {
                if (isPlaying) {
                    player?.pause()
                    isPlaying = false
                } else {
                    if (player == null) {
                        player = MediaPlayer().apply {
                            setDataSource(path)
                            prepare()
                            setOnCompletionListener {
                                isPlaying = false
                                sliderPosition = 0f
                            }
                        }
                    }
                    player?.start()
                    isPlaying = true
                }
            },
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = sliderPosition,
            onValueChange = { value ->
                isSeeking = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                player?.let { mp ->
                    mp.seekTo((sliderPosition * mp.duration).toInt())
                }
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentMs = if (player != null) {
                (sliderPosition * (player?.duration ?: 0)).toLong()
            } else {
                (sliderPosition * durationMs).toLong()
            }
            Text(
                text = formatDuration(currentMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
