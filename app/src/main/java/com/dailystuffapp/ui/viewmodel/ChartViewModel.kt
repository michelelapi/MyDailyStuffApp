package com.dailystuffapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.repository.TaskRepository
import com.dailystuffapp.domain.DayOfWeek
import com.dailystuffapp.domain.TaskState
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DayCompletionData(
        val date: LocalDate,
        val completionPercentage: Float // 0.0 to 1.0
)

class ChartViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _completionData = MutableStateFlow<List<DayCompletionData>>(emptyList())
    val completionData: StateFlow<List<DayCompletionData>> = _completionData.asStateFlow()

    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks.asStateFlow()

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    val selectedTaskId: StateFlow<Long?> = _selectedTaskId.asStateFlow()

    init {
        android.util.Log.d("ChartViewModel", "Loading completion data...")
        loadAllTasks()
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            repository.getAllTasks().collect { tasks -> _allTasks.value = tasks }
        }
        // Always reload completion data when tasks are loaded or changed
        loadCompletionData()
    }

    fun loadCompletionData() {
        viewModelScope.launch {
            val endDate = LocalDate.now()
            // Load data for the last 5 years (or all available data)
            val startDate =
                    endDate.minusYears(5).withDayOfYear(1) // Start from January 1st, 5 years ago

            val completions = repository.getCompletionsBetweenDates(startDate, endDate)
            val tasks = _allTasks.value
            android.util.Log.d("ChartViewModel", "Found ${tasks.size} tasks")
            val selectedTaskId = _selectedTaskId.value

            val tasksToConsider =
                    if (selectedTaskId != null) {
                        tasks.filter { it.id == selectedTaskId }
                    } else {
                        tasks
                    }

            // Group completions by date string for quick lookup
            val completionMap = completions.groupBy { it.date }

            val data = mutableListOf<DayCompletionData>()
            var currentDate = startDate

            while (!currentDate.isAfter(endDate)) {
                val dateString = repository.formatDate(currentDate)
                val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(currentDate.dayOfWeek)

                // Get tasks valid for this day
                val validTasksForDay =
                        tasksToConsider.filter { task ->
                            val validDays = DayOfWeek.parseDaysString(task.validDays)
                            validDays.contains(dayOfWeek)
                        }

                if (validTasksForDay.isNotEmpty()) {
                    val dayCompletions = completionMap[dateString] ?: emptyList()
                    // Calculate completion score: DONE = 1.0, PARTIALLY_DONE = 0.5
                    var totalScore = 0f
                    validTasksForDay.forEach { task ->
                        val completion = dayCompletions.find { it.taskId == task.id }
                        totalScore +=
                                when (completion?.state) {
                                    TaskState.DONE -> 1.0f
                                    TaskState.PARTIALLY_DONE -> 0.5f
                                    else -> 0f
                                }
                    }
                    val percentage =
                            if (validTasksForDay.size > 0) {
                                totalScore / validTasksForDay.size
                            } else {
                                0f
                            }
                    data.add(DayCompletionData(currentDate, percentage))
                } else {
                    // Even if no valid tasks for this day, add it with 0% for consistency
                    data.add(DayCompletionData(currentDate, 0f))
                }

                currentDate = currentDate.plusDays(1)
            }

            val todayData = data.find { it.date == LocalDate.now() }

            _completionData.value = data
        }
    }

    fun refresh() {
        loadCompletionData()
    }

    fun setSelectedTask(taskId: Long?) {
        _selectedTaskId.value = taskId
        loadCompletionData()
    }

    fun getCompletionDataForYear(): List<List<DayCompletionData>> {
        val allData = _completionData.value
        if (allData.isEmpty()) return emptyList()

        // Create a map for quick lookup
        val dataMap = allData.associateBy { it.date }

        // Get the date range from the data
        val endDate = allData.maxOfOrNull { it.date } ?: LocalDate.now()
        val startDate = allData.minOfOrNull { it.date } ?: LocalDate.now().minusYears(1)

        // Find the first Monday on or before the start date
        var firstMonday = startDate
        while (firstMonday.dayOfWeek.value != 1) { // 1 = Monday
            firstMonday = firstMonday.minusDays(1)
        }

        // Find the last Sunday on or after the end date
        var lastSunday = endDate
        while (lastSunday.dayOfWeek.value != 7) { // 7 = Sunday
            lastSunday = lastSunday.plusDays(1)
        }

        val weeks = mutableListOf<MutableList<DayCompletionData>>()
        var currentDate = firstMonday

        // Generate all weeks from firstMonday to lastSunday
        while (!currentDate.isAfter(lastSunday)) {
            val week = mutableListOf<DayCompletionData>()

            // Create a week (7 days, Monday to Sunday)
            for (i in 0..6) {
                val date = currentDate.plusDays(i.toLong())
                // Only include days that are within our date range
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    val dayData = dataMap[date] ?: DayCompletionData(date, 0f)
                    week.add(dayData)
                } else {
                    // Add placeholder for days outside range
                    week.add(DayCompletionData(date, 0f))
                }
            }

            weeks.add(week)
            currentDate = currentDate.plusDays(7)

            // Safety check to avoid infinite loops (max 10 years = ~520 weeks)
            if (weeks.size > 520) {
                break
            }
        }

        return weeks
    }

    fun getYearsInData(): List<Int> {
        val allData = _completionData.value
        if (allData.isEmpty()) return emptyList()

        // Only return years that have actual completions (percentage > 0)
        return allData
                .filter { it.completionPercentage > 0f }
                .map { it.date.year }
                .distinct()
                .sorted()
    }
}

class ChartViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ChartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
