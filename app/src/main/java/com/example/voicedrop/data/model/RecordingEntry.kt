package com.example.voicedrop.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_name")
    val userName: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "zip_path")
    val zipPath: String,

    @ColumnInfo(name = "date_time_millis")
    val dateTimeMillis: Long,

    @ColumnInfo(name = "upload_status")
    val uploadStatus: UploadStatus = UploadStatus.PENDING
)
