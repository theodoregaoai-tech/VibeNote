package com.xz.vibenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
                val viewModel: NoteViewModel = viewModel()
                val selectedNote by viewModel.selectedNote.collectAsState()
                val currentNote = selectedNote

                if (currentNote != null) {
                    NoteDetailScreen(note = currentNote, viewModel = viewModel)
                } else {
                    NoteScreen(viewModel = viewModel)
                }
            }
        }
    }
}
