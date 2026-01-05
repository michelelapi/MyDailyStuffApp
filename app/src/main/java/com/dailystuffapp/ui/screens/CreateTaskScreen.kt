package com.dailystuffapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailystuffapp.domain.DayOfWeek
import com.dailystuffapp.domain.TaskType
import com.dailystuffapp.ui.viewmodel.CreateTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(viewModel: CreateTaskViewModel, onNavigateBack: () -> Unit) {
    var taskName by remember { mutableStateOf(viewModel.taskName) }
    var taskType by remember { mutableStateOf(viewModel.taskType) }
    var targetValueInt by remember { mutableIntStateOf(viewModel.targetValue) }
    var targetValueString by remember { mutableStateOf(viewModel.targetValue.toString()) }
    var selectedDays by remember { mutableStateOf(viewModel.selectedDays.toSet()) }

    // Sync values when task type changes
    androidx.compose.runtime.LaunchedEffect(taskType) {
        targetValueString = targetValueInt.toString()
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Create Task") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
            )

            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                            text = "Task Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                                selected = taskType == TaskType.NUMBER_TASK,
                                onClick = { taskType = TaskType.NUMBER_TASK }
                        )
                        Text(text = "Number Task", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                                selected = taskType == TaskType.TIME_TASK,
                                onClick = { taskType = TaskType.TIME_TASK }
                        )
                        Text(text = "Time Task", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            OutlinedTextField(
                    value = targetValueString,
                    onValueChange = { value ->
                        // Only allow digits, allow empty string for time tasks
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            targetValueString = value
                            targetValueInt = value.toIntOrNull() ?: 0
                        }
                    },
                    label = {
                        Text(
                                if (taskType == TaskType.NUMBER_TASK) "Target Count"
                                else "Target Minutes"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
            )

            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                text = "Valid Days",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Button(
                                onClick = {
                                    if (selectedDays.size == DayOfWeek.values().size) {
                                        selectedDays = emptySet()
                                    } else {
                                        selectedDays = DayOfWeek.values().toSet()
                                    }
                                },
                                modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                    text =
                                            if (selectedDays.size == DayOfWeek.values().size)
                                                    "Clear All"
                                            else "Select All",
                                    style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    DayOfWeek.values().forEach { day ->
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                    checked = selectedDays.contains(day),
                                    onCheckedChange = { checked ->
                                        selectedDays =
                                                if (checked) {
                                                    selectedDays + day
                                                } else {
                                                    selectedDays - day
                                                }
                                    }
                            )
                            Text(
                                    text =
                                            day.value.lowercase().replaceFirstChar {
                                                it.uppercase()
                                            },
                                    modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                    onClick = {
                        viewModel.taskName = taskName
                        viewModel.taskType = taskType
                        val finalTargetValue = targetValueString.toIntOrNull() ?: 0
                        viewModel.targetValue = finalTargetValue
                        viewModel.selectedDays.clear()
                        viewModel.selectedDays.addAll(selectedDays)
                        viewModel.saveTask { onNavigateBack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                            taskName.isNotBlank() &&
                                    selectedDays.isNotEmpty() &&
                                    (targetValueString.toIntOrNull() ?: 0 > 0)
            ) { Text("Save Task") }
        }
    }
}
