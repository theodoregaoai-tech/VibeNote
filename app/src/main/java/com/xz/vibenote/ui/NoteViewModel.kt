package com.xz.vibenote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xz.vibenote.data.Note
import com.xz.vibenote.data.NoteDatabase
import com.xz.vibenote.data.NoteRepository
import com.xz.vibenote.sync.NoteSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

sealed class Screen {
    data object Voice : Screen()
    data object Notes : Screen()
}

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    private val _userId = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val dailyNotes: StateFlow<List<DailyNotes>>

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Voice)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote: StateFlow<Note?> = _editingNote.asStateFlow()

    private val _selectedNoteId = MutableStateFlow<Long?>(null)
    val selectedNote: StateFlow<Note?>

    private val userNotes = _userId.flatMapLatest { uid ->
        if (uid != null) repository.getNotesForUser(uid)
        else flowOf(emptyList())
    }

    init {
        val dao = NoteDatabase.getDatabase(application).noteDao()
        val syncManager = NoteSyncManager(dao, viewModelScope)
        repository = NoteRepository(dao, syncManager, viewModelScope)

        dailyNotes = combine(_searchQuery, userNotes) { query, notes ->
            if (query.isBlank()) notes
            else notes.filter { it.content.contains(query, ignoreCase = true) }
        }
            .map { notes -> groupNotesByDate(notes) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        selectedNote = combine(_selectedNoteId, userNotes) { id, notes ->
            id?.let { noteId -> notes.find { it.id == noteId } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun onUserSignedIn(userId: String) {
        if (_userId.value == userId) return
        _userId.value = userId
        viewModelScope.launch {
            repository.claimNotesForUser(userId)
            repository.startSync(userId)
        }
    }

    fun onUserSignedOut() {
        repository.stopSync()
        _userId.value = null
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
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
            val userId = _userId.value
            if (editing != null) {
                repository.update(editing.copy(content = text), userId)
                _editingNote.value = null
            } else {
                repository.insert(Note(content = text), userId)
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
            repository.delete(note, _userId.value)
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

    fun navigateToVoice() {
        _currentScreen.value = Screen.Voice
    }

    fun navigateToNotes() {
        _currentScreen.value = Screen.Notes
    }

    fun saveVoiceNote(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insert(Note(content = trimmed), _userId.value)
            _currentScreen.value = Screen.Notes
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSync()
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
