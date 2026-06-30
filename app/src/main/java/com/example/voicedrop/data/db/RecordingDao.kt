package com.example.voicedrop.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.voicedrop.data.model.RecordingEntry
import com.example.voicedrop.data.model.UploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert
    suspend fun insert(entry: RecordingEntry): Long

    // Use actual DB column names from @ColumnInfo, not Kotlin property names
    @Query("SELECT * FROM recordings ORDER BY date_time_millis DESC")
    fun getAll(): Flow<List<RecordingEntry>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntry?

    @Query("UPDATE recordings SET upload_status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: UploadStatus)

    @Query("SELECT * FROM recordings WHERE upload_status = 'FAILED'")
    suspend fun getFailedEntries(): List<RecordingEntry>

    @Delete
    suspend fun delete(entry: RecordingEntry)

    @Query("DELETE FROM recordings")
    suspend fun deleteAll()
}
