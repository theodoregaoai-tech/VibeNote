package com.xz.vibenote.data

import com.xz.vibenote.sync.NoteSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteRepository(
    private val noteDao: NoteDao,
    private val syncManager: NoteSyncManager,
    private val scope: CoroutineScope
) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesForUser(userId: String): Flow<List<Note>> =
        noteDao.getNotesForUser(userId)

    suspend fun insert(note: Note, userId: String?) {
        val noteWithUser = note.copy(userId = userId, isSynced = false)
        noteDao.insert(noteWithUser)
        if (userId != null) {
            scope.launch { syncManager.pushUnsyncedNotes(userId) }
        }
    }

    suspend fun update(note: Note, userId: String?) {
        val updated = note.copy(userId = userId, isSynced = false)
        noteDao.update(updated)
        if (userId != null) {
            scope.launch { syncManager.pushUnsyncedNotes(userId) }
        }
    }

    suspend fun delete(note: Note, userId: String?) {
        noteDao.delete(note)
        if (userId != null && note.firestoreId != null) {
            scope.launch { syncManager.pushDeleteToFirestore(userId, note.firestoreId) }
        }
    }

    suspend fun claimNotesForUser(userId: String) {
        noteDao.claimNotesForUser(userId)
        syncManager.pushUnsyncedNotes(userId)
    }

    fun startSync(userId: String) {
        syncManager.startListening(userId)
    }

    fun stopSync() {
        syncManager.stopListening()
    }
}
