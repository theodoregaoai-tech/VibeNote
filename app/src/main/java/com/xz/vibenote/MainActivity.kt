package com.xz.vibenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xz.vibenote.auth.AuthViewModel
import com.xz.vibenote.ui.AuthScreen
import com.xz.vibenote.ui.NoteDetailScreen
import com.xz.vibenote.ui.NoteScreen
import com.xz.vibenote.ui.NoteViewModel
import com.xz.vibenote.ui.Screen
import com.xz.vibenote.ui.VoiceScreen
import com.xz.vibenote.ui.theme.VibeNoteTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeNoteTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.uiState.collectAsState()

                if (authState.isLoading && authState.user == null) {
                    return@VibeNoteTheme
                }

                if (authState.isSignedIn) {
                    val noteViewModel: NoteViewModel = viewModel()

                    LaunchedEffect(authState.user?.uid) {
                        authState.user?.uid?.let { uid ->
                            noteViewModel.onUserSignedIn(uid)
                        }
                    }

                    val selectedNote by noteViewModel.selectedNote.collectAsState()
                    val currentNote = selectedNote
                    val currentScreen by noteViewModel.currentScreen.collectAsState()

                    if (currentNote != null) {
                        NoteDetailScreen(note = currentNote, viewModel = noteViewModel)
                    } else {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            "VibeNote",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            noteViewModel.onUserSignedOut()
                                            authViewModel.signOut()
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ExitToApp,
                                                contentDescription = "Sign out"
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Voice,
                                        onClick = noteViewModel::navigateToVoice,
                                        icon = {
                                            Icon(Icons.Default.Mic, contentDescription = null)
                                        },
                                        label = { Text("Home") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Notes,
                                        onClick = noteViewModel::navigateToNotes,
                                        icon = {
                                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                        },
                                        label = { Text("Notes") }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                when (currentScreen) {
                                    is Screen.Voice -> VoiceScreen(viewModel = noteViewModel)
                                    is Screen.Notes -> NoteScreen(viewModel = noteViewModel)
                                }
                            }
                        }
                    }
                } else {
                    AuthScreen(authViewModel = authViewModel)
                }
            }
        }
    }
}
