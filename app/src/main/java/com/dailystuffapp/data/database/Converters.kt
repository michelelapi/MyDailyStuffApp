package com.dailystuffapp.data.database

import androidx.room.TypeConverter
import com.dailystuffapp.domain.TaskState
import com.dailystuffapp.domain.TaskType

class Converters {
    @TypeConverter
    fun fromTaskType(value: TaskType): String {
        return value.name
    }

    @TypeConverter
    fun toTaskType(value: String): TaskType {
        return TaskType.valueOf(value)
    }

    @TypeConverter
    fun fromTaskState(value: TaskState): String {
        return value.name
    }

    @TypeConverter
    fun toTaskState(value: String): TaskState {
        return TaskState.valueOf(value)
    }
}

