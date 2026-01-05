package com.dailystuffapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.repository.TaskRepository
import com.dailystuffapp.domain.DayOfWeek
import com.dailystuffapp.domain.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditTasksViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks.asStateFlow()

    init {
        loadAllTasks()
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            repository.getAllTasks().collect { tasks ->
                _allTasks.value = tasks
            }
        }
    }

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    var taskName: String = ""
    var taskType: TaskType = TaskType.NUMBER_TASK
    var targetValue: Int = 1
    var selectedDays: MutableSet<DayOfWeek> = mutableSetOf()

    fun startEditingTask(task: Task) {
        _editingTask.value = task
        taskName = task.name
        taskType = task.type
        targetValue = task.targetValue
        selectedDays = DayOfWeek.parseDaysString(task.validDays).toMutableSet()
    }

    fun cancelEditing() {
        _editingTask.value = null
        reset()
    }

    fun updateTask(onSuccess: () -> Unit) {
        val task = _editingTask.value ?: return
        if (taskName.isBlank()) {
            return
        }
        if (selectedDays.isEmpty()) {
            return
        }
        if (targetValue <= 0) {
            return
        }

        viewModelScope.launch {
            val validDaysString = DayOfWeek.daysToString(selectedDays.toList())
            val updatedTask = task.copy(
                name = taskName,
                type = taskType,
                targetValue = targetValue,
                validDays = validDaysString
            )
            repository.updateTask(updatedTask)
            _editingTask.value = null
            reset()
            onSuccess()
        }
    }

    fun deleteTask(task: Task, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTask(task)
            onSuccess()
        }
    }

    fun reset() {
        taskName = ""
        taskType = TaskType.NUMBER_TASK
        targetValue = 1
        selectedDays.clear()
    }

    fun toggleDay(day: DayOfWeek) {
        if (selectedDays.contains(day)) {
            selectedDays.remove(day)
        } else {
            selectedDays.add(day)
        }
    }
}

class EditTasksViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditTasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditTasksViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

