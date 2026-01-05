package com.dailystuffapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {
    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND date = :date")
    suspend fun getCompletion(taskId: Long, date: String): TaskCompletion?

    @Query("SELECT * FROM task_completions WHERE date = :date")
    suspend fun getCompletionsForDate(date: String): List<TaskCompletion>

    @Query("SELECT * FROM task_completions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getCompletionsBetweenDates(startDate: String, endDate: String): List<TaskCompletion>

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getCompletionsForTaskBetweenDates(
        taskId: Long,
        startDate: String,
        endDate: String
    ): List<TaskCompletion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletion(completion: TaskCompletion)

    @Update
    suspend fun updateCompletion(completion: TaskCompletion)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun deleteCompletionsForTask(taskId: Long)

    @Query("DELETE FROM task_completions WHERE date = :date")
    suspend fun deleteCompletionsForDate(date: String)

    @Query("SELECT * FROM task_completions ORDER BY date ASC")
    suspend fun getAllCompletions(): List<TaskCompletion>
}

