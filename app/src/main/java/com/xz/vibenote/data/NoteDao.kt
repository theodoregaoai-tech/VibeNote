package com.xz.vibenote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun getUnsyncedNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getNoteByFirestoreId(firestoreId: String): Note?

    @Query("UPDATE notes SET isSynced = 1, firestoreId = :firestoreId WHERE id = :localId")
    suspend fun markSynced(localId: Long, firestoreId: String)

    @Query("UPDATE notes SET userId = :userId, isSynced = 0 WHERE userId IS NULL")
    suspend fun claimNotesForUser(userId: String)

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotesForUser(userId: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long
}
