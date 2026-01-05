package com.dailystuffapp.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dailystuffapp.domain.TaskState

@Entity(
    tableName = "task_completions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId", "date"], unique = true)]
)
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val date: String, // format: "YYYY-MM-DD"
    val state: TaskState,
    val actualValue: Int, // actual count/time achieved
    val lastUpdated: Long = System.currentTimeMillis()
)

