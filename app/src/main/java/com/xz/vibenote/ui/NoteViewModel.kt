package com.xz.vibenote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xz.vibenote.data.Note
import com.xz.vibenote.data.NoteDatabase
import com.xz.vibenote.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyNotes(
    val dateLabel: String,
    val notes: List<Note>
)

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    val dailyNotes: StateFlow<List<DailyNotes>>

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    private val _selectedNoteId = MutableStateFlow<Long?>(null)
    val selectedNote: StateFlow<Note?>

    init {
        val dao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(dao)

        dailyNotes = repository.allNotes
            .map { notes -> groupNotesByDate(notes) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        selectedNote = combine(_selectedNoteId, repository.allNotes) { id, notes ->
            id?.let { noteId -> notes.find { it.id == noteId } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun onTextChange(text: String) {
        _currentText.value = text
    }

    fun onVoiceResult(text: String) {
        val current = _currentText.value
        _currentText.value = if (current.isBlank()) text else "$current $text"
    }

    fun saveNote() {
        val text = _currentText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            val editing = _editingNote.value
            if (editing != null) {
                repository.update(editing.copy(content = text))
                _editingNote.value = null
            } else {
                repository.insert(Note(content = text))
            }
            _currentText.value = ""
        }
    }

    fun selectNote(note: Note) {
        _selectedNoteId.value = note.id
    }

    fun clearSelection() {
        _selectedNoteId.value = null
    }

    fun deleteNote(note: Note) {
        if (_selectedNoteId.value == note.id) {
            _selectedNoteId.value = null
        }
        viewModelScope.launch {
            repository.delete(note)
        }
    }

    fun startEditing(note: Note) {
        _selectedNoteId.value = null
        _editingNote.value = note
        _currentText.value = note.content
    }

    fun cancelEditing() {
        _editingNote.value = null
        _currentText.value = ""
    }

    private fun groupNotesByDate(notes: List<Note>): List<DailyNotes> {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val yesterdayStr = dateFormat.format(Date(System.currentTimeMillis() - 86400000))

        return notes
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .map { (dateStr, notesForDate) ->
                val label = when (dateStr) {
                    todayStr -> "Today"
                    yesterdayStr -> "Yesterday"
                    else -> dateStr
                }
                DailyNotes(label, notesForDate)
            }
    }
}
