package com.dailystuffapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.database.TaskCompletion
import com.dailystuffapp.data.repository.TaskRepository
import com.dailystuffapp.domain.TaskState
import com.dailystuffapp.domain.TaskType
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskWithCompletion(val task: Task, val completion: TaskCompletion?)

class MainViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _tasksWithCompletions = MutableStateFlow<List<TaskWithCompletion>>(emptyList())
    val tasksWithCompletions: StateFlow<List<TaskWithCompletion>> =
            _tasksWithCompletions.asStateFlow()

    // For debug: current date being viewed (defaults to today)
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    init {
        loadTasksForDate(_currentDate.value)
    }

    private fun loadTasksForDate(date: LocalDate) {
        viewModelScope.launch {
            repository.getTasksForDateFlow(date).collect { tasks ->
                val tasksWithCompletions =
                        tasks.map { task ->
                            val completion = repository.getCompletion(task.id, date)
                            TaskWithCompletion(task, completion)
                        }
                _tasksWithCompletions.value = tasksWithCompletions
            }
        }
    }

    fun onTaskClicked(taskId: Long) {
        viewModelScope.launch {
            val currentList = _tasksWithCompletions.value.toMutableList()
            val taskIndex = currentList.indexOfFirst { it.task.id == taskId }
            if (taskIndex == -1) return@launch

            val taskWithCompletion = currentList[taskIndex]
            val task = taskWithCompletion.task
            val existingCompletion = taskWithCompletion.completion
            val currentDate = _currentDate.value
            val dateString = repository.formatDate(currentDate)

            when (task.type) {
                TaskType.NUMBER_TASK -> {
                    val currentCount = existingCompletion?.actualValue ?: 0
                    val newCount = currentCount + 1
                    val newState =
                            when {
                                newCount >= task.targetValue -> TaskState.DONE
                                newCount > 0 -> TaskState.PARTIALLY_DONE
                                else -> TaskState.NOT_DONE
                            }

                    val completion =
                            TaskCompletion(
                                    id = existingCompletion?.id ?: 0,
                                    taskId = task.id,
                                    date = dateString,
                                    state = newState,
                                    actualValue = newCount
                            )
                    repository.insertOrUpdateCompletion(completion)
                    // Update the list in place to preserve order
                    currentList[taskIndex] = TaskWithCompletion(task, completion)
                    _tasksWithCompletions.value = currentList
                }
                TaskType.TIME_TASK -> {
                    val completion =
                            TaskCompletion(
                                    id = existingCompletion?.id ?: 0,
                                    taskId = task.id,
                                    date = dateString,
                                    state = TaskState.DONE,
                                    actualValue = task.targetValue // default to target time
                            )
                    repository.insertOrUpdateCompletion(completion)
                    // Update the list in place to preserve order
                    currentList[taskIndex] = TaskWithCompletion(task, completion)
                    _tasksWithCompletions.value = currentList
                }
            }
        }
    }

    fun updateTaskTime(taskId: Long, actualMinutes: Int) {
        viewModelScope.launch {
            val currentList = _tasksWithCompletions.value.toMutableList()
            val taskIndex = currentList.indexOfFirst { it.task.id == taskId }
            if (taskIndex == -1) return@launch

            val taskWithCompletion = currentList[taskIndex]
            val task = taskWithCompletion.task
            val existingCompletion = taskWithCompletion.completion
            val currentDate = _currentDate.value
            val dateString = repository.formatDate(currentDate)

            if (task.type == TaskType.TIME_TASK) {
                val newState =
                        when {
                            actualMinutes >= task.targetValue -> TaskState.DONE
                            actualMinutes > 0 -> TaskState.PARTIALLY_DONE
                            else -> TaskState.NOT_DONE
                        }

                val completion =
                        TaskCompletion(
                                id = existingCompletion?.id ?: 0,
                                taskId = task.id,
                                date = dateString,
                                state = newState,
                                actualValue = actualMinutes
                        )
                repository.insertOrUpdateCompletion(completion)
                // Update the list in place to preserve order
                currentList[taskIndex] = TaskWithCompletion(task, completion)
                _tasksWithCompletions.value = currentList
            }
        }
    }

    private fun refreshTasksForCurrentDate() {
        viewModelScope.launch {
            val currentDate = _currentDate.value
            val tasks = repository.getTasksForDate(currentDate)
            val tasksWithCompletions =
                    tasks.map { task ->
                        val completion = repository.getCompletion(task.id, currentDate)
                        TaskWithCompletion(task, completion)
                    }
            _tasksWithCompletions.value = tasksWithCompletions
        }
    }

    fun refreshTasks() {
        loadTasksForDate(_currentDate.value)
    }

    // Debug methods
    fun navigateToPreviousDay() {
        _currentDate.value = _currentDate.value.minusDays(1)
        loadTasksForDate(_currentDate.value)
    }

    fun navigateToNextDay() {
        _currentDate.value = _currentDate.value.plusDays(1)
        loadTasksForDate(_currentDate.value)
    }

    fun resetCurrentDay() {
        viewModelScope.launch {
            repository.resetDay(_currentDate.value)
            refreshTasksForCurrentDate()
        }
    }
}

class MainViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
