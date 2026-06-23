package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isAppointment: Boolean,
    val dueTimestamp: Long,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val isCompleted: Boolean = false,
    val alertScheduled: Boolean = false,
    val reminderTimestamp: Long = 0L
)
