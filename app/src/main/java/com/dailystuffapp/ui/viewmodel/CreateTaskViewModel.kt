package com.dailystuffapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.repository.TaskRepository
import com.dailystuffapp.domain.DayOfWeek
import com.dailystuffapp.domain.TaskType
import kotlinx.coroutines.launch

class CreateTaskViewModel(private val repository: TaskRepository) : ViewModel() {
    var taskName: String = ""
    var taskType: TaskType = TaskType.NUMBER_TASK
    var targetValue: Int = 1
    var selectedDays: MutableSet<DayOfWeek> = mutableSetOf()

    fun selectAllDays() {
        selectedDays.clear()
        selectedDays.addAll(DayOfWeek.values().toList())
    }

    fun toggleDay(day: DayOfWeek) {
        if (selectedDays.contains(day)) {
            selectedDays.remove(day)
        } else {
            selectedDays.add(day)
        }
    }

    fun saveTask(onSuccess: () -> Unit) {
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
            val task = Task(
                name = taskName,
                type = taskType,
                targetValue = targetValue,
                validDays = validDaysString
            )
            repository.insertTask(task)
            onSuccess()
        }
    }

    fun reset() {
        taskName = ""
        taskType = TaskType.NUMBER_TASK
        targetValue = 1
        selectedDays.clear()
    }
}

class CreateTaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateTaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateTaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

