package com.example.voicedrop.data.db

import androidx.room.TypeConverter
import com.example.voicedrop.data.model.UploadStatus

class Converters {

    @TypeConverter
    fun fromUploadStatus(status: UploadStatus): String = status.name

    @TypeConverter
    fun toUploadStatus(value: String): UploadStatus = UploadStatus.valueOf(value)
}
