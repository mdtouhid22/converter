package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val inputValue: String,
    val sourceBase: Int,
    val targetBase: Int,
    val resultValue: String,
    val isExact: Boolean = true,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
