package com.xz.vibenote.sync

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.xz.vibenote.data.Note
import com.xz.vibenote.data.NoteDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class NoteSyncManager(
    private val noteDao: NoteDao,
    private val scope: CoroutineScope
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var snapshotListener: ListenerRegistration? = null
    private val pushMutex = Mutex()

    companion object {
        private const val TAG = "NoteSyncManager"
    }

    private fun notesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("notes")

    // --- Push: Local -> Firestore ---

    suspend fun pushUnsyncedNotes(userId: String) = pushMutex.withLock {
        val unsynced = noteDao.getUnsyncedNotes()
        for (note in unsynced) {
            try {
                val noteMap = mapOf(
                    "content" to note.content,
                    "timestamp" to note.timestamp
                )
                if (note.firestoreId != null) {
                    notesCollection(userId).document(note.firestoreId)
                        .set(noteMap, SetOptions.merge()).await()
                    noteDao.markSynced(note.id, note.firestoreId)
                } else {
                    val docRef = notesCollection(userId).add(noteMap).await()
                    noteDao.markSynced(note.id, docRef.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync note ${note.id}", e)
            }
        }
    }

    suspend fun pushDeleteToFirestore(userId: String, firestoreId: String) {
        try {
            notesCollection(userId).document(firestoreId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete note from Firestore: $firestoreId", e)
        }
    }

    // --- Pull: Firestore -> Local ---

    fun startListening(userId: String) {
        stopListening()

        snapshotListener = notesCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val firestoreId = doc.id
                        val content = doc.getString("content") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val existing = noteDao.getNoteByFirestoreId(firestoreId)
                                if (existing != null) {
                                    if (timestamp > existing.timestamp) {
                                        noteDao.update(
                                            existing.copy(
                                                content = content,
                                                timestamp = timestamp,
                                                isSynced = true
                                            )
                                        )
                                    }
                                } else {
                                    // Check for a local note with matching content that
                                    // was created by a concurrent push (race between
                                    // multiple pushUnsyncedNotes or this listener).
                                    val duplicate = noteDao.findDuplicateNote(userId, content, timestamp)
                                    if (duplicate != null) {
                                        noteDao.markSynced(duplicate.id, firestoreId)
                                    } else {
                                        noteDao.insert(
                                            Note(
                                                firestoreId = firestoreId,
                                                userId = userId,
                                                content = content,
                                                timestamp = timestamp,
                                                isSynced = true
                                            )
                                        )
                                    }
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val existing = noteDao.getNoteByFirestoreId(firestoreId)
                                if (existing != null) {
                                    noteDao.delete(existing)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
    }
}
