package com.dailystuffapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.database.TaskCompletion
import com.dailystuffapp.domain.TaskType

@Composable
fun TaskDetailDialog(
        task: Task,
        completion: TaskCompletion?,
        onDismiss: () -> Unit,
        onSave: (Int) -> Unit
) {
    val initialValue = completion?.actualValue?.takeIf { it > 0 } ?: task.targetValue
    var timeInput by remember { mutableStateOf(initialValue.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                        text = task.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                )

                when (task.type) {
                    TaskType.TIME_TASK -> {
                        Text(
                                text = "Target: ${task.targetValue} minutes",
                                style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                                value = timeInput,
                                onValueChange = { value ->
                                    // Only allow digits, allow empty string
                                    if (value.isEmpty() || value.all { it.isDigit() }) {
                                        timeInput = value
                                    }
                                },
                                label = { Text("Actual time (minutes)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                        )
                    }
                    TaskType.NUMBER_TASK -> {
                        Text(
                                text = "Target: ${task.targetValue} times",
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text =
                                        "Current: ${completion?.actualValue ?: 0} of ${task.targetValue}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (task.type == TaskType.TIME_TASK) {
                        Button(
                                onClick = {
                                    val value = timeInput.toIntOrNull() ?: task.targetValue
                                    onSave(value)
                                }
                        ) { Text("Save") }
                    } else {
                        Button(onClick = onDismiss) { Text("OK") }
                    }
                }
            }
        }
    }
}
