package com.dailystuffapp.data.repository

import com.dailystuffapp.data.database.AppDatabase
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.database.TaskCompletion
import com.dailystuffapp.domain.DayOfWeek
import com.dailystuffapp.domain.TaskState
import com.dailystuffapp.domain.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskRepository(private val database: AppDatabase) {
    private val taskDao = database.taskDao()
    private val completionDao = database.taskCompletionDao()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        completionDao.deleteCompletionsForTask(task.id)
    }

    suspend fun getTasksForToday(): List<Task> {
        val today = LocalDate.now()
        val todayDayOfWeek = DayOfWeek.fromJavaDayOfWeek(today.dayOfWeek)
        val dayPattern = "%${todayDayOfWeek.value}%"
        val allTasks = taskDao.getTasksForDay(dayPattern)
        
        return allTasks.filter { task ->
            val validDays = DayOfWeek.parseDaysString(task.validDays)
            validDays.contains(todayDayOfWeek)
        }
    }

    fun getTasksForTodayFlow(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { tasks ->
            val today = LocalDate.now()
            val todayDayOfWeek = DayOfWeek.fromJavaDayOfWeek(today.dayOfWeek)
            tasks.filter { task ->
                val validDays = DayOfWeek.parseDaysString(task.validDays)
                validDays.contains(todayDayOfWeek)
            }
        }
    }

    suspend fun getCompletion(taskId: Long, date: LocalDate): TaskCompletion? {
        val dateString = date.format(dateFormatter)
        return completionDao.getCompletion(taskId, dateString)
    }

    suspend fun getCompletionForToday(taskId: Long): TaskCompletion? {
        val today = LocalDate.now()
        return getCompletion(taskId, today)
    }

    suspend fun insertOrUpdateCompletion(completion: TaskCompletion) {
        completionDao.insertOrUpdateCompletion(completion)
    }

    suspend fun getCompletionsForDate(date: LocalDate): List<TaskCompletion> {
        val dateString = date.format(dateFormatter)
        return completionDao.getCompletionsForDate(dateString)
    }

    suspend fun getCompletionsBetweenDates(startDate: LocalDate, endDate: LocalDate): List<TaskCompletion> {
        val startString = startDate.format(dateFormatter)
        val endString = endDate.format(dateFormatter)
        return completionDao.getCompletionsBetweenDates(startString, endString)
    }

    suspend fun getCompletionsForTaskBetweenDates(
        taskId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TaskCompletion> {
        val startString = startDate.format(dateFormatter)
        val endString = endDate.format(dateFormatter)
        return completionDao.getCompletionsForTaskBetweenDates(taskId, startString, endString)
    }

    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun parseDate(dateString: String): LocalDate {
        return LocalDate.parse(dateString, dateFormatter)
    }

    suspend fun getTasksForDate(date: LocalDate): List<Task> {
        val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(date.dayOfWeek)
        val dayPattern = "%${dayOfWeek.value}%"
        val allTasks = taskDao.getTasksForDay(dayPattern)
        
        return allTasks.filter { task ->
            val validDays = DayOfWeek.parseDaysString(task.validDays)
            validDays.contains(dayOfWeek)
        }
    }

    fun getTasksForDateFlow(date: LocalDate): Flow<List<Task>> {
        return taskDao.getAllTasks().map { tasks ->
            val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(date.dayOfWeek)
            tasks.filter { task ->
                val validDays = DayOfWeek.parseDaysString(task.validDays)
                validDays.contains(dayOfWeek)
            }
        }
    }

    suspend fun resetDay(date: LocalDate) {
        val dateString = date.format(dateFormatter)
        completionDao.deleteCompletionsForDate(dateString)
    }
}

