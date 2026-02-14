package com.xz.vibenote.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class RecordingState { Idle, Recording, Review }

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun createRecognitionIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
}

private fun createRecognitionListener(
    onPartial: (String) -> Unit,
    onFinalResult: (String) -> Unit,
    onError: (Int) -> Unit
): RecognitionListener = object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onPartial(matches[0])
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onFinalResult(matches[0])
        }
    }

    override fun onError(error: Int) {
        onError(error)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

@Composable
fun VoiceScreen(viewModel: NoteViewModel) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(RecordingState.Idle) }
    var transcribedText by remember { mutableStateOf("") }
    var partialText by remember { mutableStateOf("") }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var shouldListen by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    fun startListening() {
        val recognizer = speechRecognizer ?: return
        recognizer.setRecognitionListener(createRecognitionListener(
            onPartial = { partial -> partialText = partial },
            onFinalResult = { result ->
                transcribedText = if (transcribedText.isBlank()) result
                else "$transcribedText $result"
                partialText = ""
                if (shouldListen) {
                    recognizer.startListening(createRecognitionIntent())
                }
            },
            onError = { error ->
                // 6 = SPEECH_TIMEOUT, 7 = NO_MATCH — restart for these
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_NO_MATCH
                ) {
                    if (shouldListen) {
                        recognizer.startListening(createRecognitionIntent())
                    }
                } else {
                    shouldListen = false
                    state = if (transcribedText.isNotBlank()) RecordingState.Review
                    else RecordingState.Idle
                }
            }
        ))
        recognizer.startListening(createRecognitionIntent())
    }

    fun stopListening() {
        shouldListen = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            shouldListen = true
            elapsedMs = 0L
            transcribedText = ""
            partialText = ""
            state = RecordingState.Recording
            startListening()
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Timer
    LaunchedEffect(state) {
        if (state == RecordingState.Recording) {
            while (true) {
                delay(1000)
                elapsedMs += 1000
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    // Pulse animation
    val pulseScale = if (state == RecordingState.Recording) {
        val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(750),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mic_scale"
        )
        scale
    } else {
        1f
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            RecordingState.Idle -> {
                FilledIconButton(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Record a note",
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Tap to record",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            RecordingState.Recording -> {
                val scrollState = rememberScrollState()
                val displayText = buildAnnotatedString {
                    append(transcribedText)
                    if (partialText.isNotEmpty()) {
                        if (transcribedText.isNotBlank()) append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))) {
                            append(partialText)
                        }
                    }
                }

                // Auto-scroll when text changes
                LaunchedEffect(displayText) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(scrollState),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                FilledIconButton(
                    onClick = {
                        stopListening()
                        state = if (transcribedText.isNotBlank()) RecordingState.Review
                        else RecordingState.Idle
                    },
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop recording",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatDuration(elapsedMs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Listening...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            RecordingState.Review -> {
                OutlinedTextField(
                    value = transcribedText,
                    onValueChange = { transcribedText = it },
                    label = { Text("Edit transcription") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Discard
                    IconButton(onClick = {
                        transcribedText = ""
                        partialText = ""
                        state = RecordingState.Idle
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Discard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    // Save
                    FilledIconButton(
                        onClick = {
                            viewModel.saveVoiceNote(transcribedText)
                            transcribedText = ""
                            partialText = ""
                            state = RecordingState.Idle
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save note")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
