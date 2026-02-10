package com.xz.vibenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xz.vibenote.auth.AuthViewModel
import com.xz.vibenote.ui.AuthScreen
import com.xz.vibenote.ui.NoteDetailScreen
import com.xz.vibenote.ui.NoteScreen
import com.xz.vibenote.ui.NoteViewModel
import com.xz.vibenote.ui.theme.VibeNoteTheme

class MainActivity : ComponentActivity() {
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

                    if (currentNote != null) {
                        NoteDetailScreen(note = currentNote, viewModel = noteViewModel)
                    } else {
                        NoteScreen(
                            viewModel = noteViewModel,
                            onSignOut = {
                                noteViewModel.onUserSignedOut()
                                authViewModel.signOut()
                            }
                        )
                    }
                } else {
                    AuthScreen(authViewModel = authViewModel)
                }
            }
        }
    }
}
