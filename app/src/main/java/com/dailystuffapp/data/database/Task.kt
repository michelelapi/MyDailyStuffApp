package com.dailystuffapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dailystuffapp.domain.TaskType

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TaskType,
    val targetValue: Int, // target count for number-task, target minutes for time-task
    val validDays: String, // comma-separated days (e.g., "MONDAY,TUESDAY,WEDNESDAY")
    val createdAt: Long = System.currentTimeMillis()
)

