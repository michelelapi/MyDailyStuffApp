package com.dailystuffapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.domain.DayOfWeek
import com.dailystuffapp.domain.TaskType
import com.dailystuffapp.ui.viewmodel.EditTasksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTasksScreen(viewModel: EditTasksViewModel, onNavigateBack: () -> Unit) {
    val allTasks by viewModel.allTasks.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    var taskName by remember { mutableStateOf(viewModel.taskName) }
    var taskType by remember { mutableStateOf(viewModel.taskType) }
    var targetValueInt by remember { mutableIntStateOf(viewModel.targetValue) }
    var targetValueString by remember { mutableStateOf(viewModel.targetValue.toString()) }
    var selectedDays by remember { mutableStateOf(viewModel.selectedDays.toSet()) }
    var showDeleteDialog by remember { mutableStateOf<Task?>(null) }

    // Update local state when editing task changes
    LaunchedEffect(editingTask) {
        if (editingTask != null) {
            taskName = viewModel.taskName
            taskType = viewModel.taskType
            targetValueInt = viewModel.targetValue
            targetValueString = viewModel.targetValue.toString()
            selectedDays = viewModel.selectedDays.toSet()
        }
    }

    // Sync values when task type changes
    LaunchedEffect(taskType) { targetValueString = targetValueInt.toString() }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Modify Tasks") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        if (editingTask != null) {
            // Show edit form
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(paddingValues)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                        text = "Edit Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )

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
                                fontWeight = FontWeight.Bold
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
                                    fontWeight = FontWeight.Bold
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

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                            onClick = { viewModel.cancelEditing() },
                            modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                            onClick = {
                                viewModel.taskName = taskName
                                viewModel.taskType = taskType
                                val finalTargetValue = targetValueString.toIntOrNull() ?: 0
                                viewModel.targetValue = finalTargetValue
                                viewModel.selectedDays.clear()
                                viewModel.selectedDays.addAll(selectedDays)
                                viewModel.updateTask {
                                    // Task updated
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled =
                                    taskName.isNotBlank() &&
                                            selectedDays.isNotEmpty() &&
                                            (targetValueString.toIntOrNull() ?: 0 > 0)
                    ) { Text("Save") }
                }
            }
        } else {
            // Show task list
            if (allTasks.isEmpty()) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                ) { Text(text = "No tasks yet", style = MaterialTheme.typography.bodyLarge) }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTasks, key = { it.id }) { task ->
                        TaskItem(
                                task = task,
                                onEdit = {
                                    viewModel.taskName = task.name
                                    viewModel.taskType = task.type
                                    viewModel.targetValue = task.targetValue
                                    viewModel.selectedDays =
                                            DayOfWeek.parseDaysString(task.validDays).toMutableSet()
                                    viewModel.startEditingTask(task)
                                },
                                onDelete = { showDeleteDialog = task }
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { task ->
            AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = { Text("Delete Task") },
                    text = {
                        Text(
                                "Are you sure you want to delete \"${task.name}\"? This action cannot be undone."
                        )
                    },
                    confirmButton = {
                        Button(
                                onClick = { viewModel.deleteTask(task) { showDeleteDialog = null } }
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        Button(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                    }
            )
        }
    }
}

@Composable
fun TaskItem(task: Task, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text =
                                "Type: ${if (task.type == TaskType.NUMBER_TASK) "Number" else "Time"}",
                        style = MaterialTheme.typography.bodySmall
                )
                Text(
                        text =
                                "Target: ${task.targetValue} ${if (task.type == TaskType.NUMBER_TASK) "count" else "minutes"}",
                        style = MaterialTheme.typography.bodySmall
                )
                val validDays = DayOfWeek.parseDaysString(task.validDays)
                Text(
                        text =
                                "Days: ${validDays.joinToString(", ") { it.value.lowercase().replaceFirstChar { char -> char.uppercase() } }}",
                        style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
