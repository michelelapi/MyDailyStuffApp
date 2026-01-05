package com.dailystuffapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.database.TaskCompletion
import com.dailystuffapp.domain.TaskState

@Composable
fun TaskButton(
    task: Task,
    completion: TaskCompletion?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = completion?.state ?: TaskState.NOT_DONE
    val backgroundColor = when (state) {
        TaskState.DONE -> Color(0xFF4CAF50) // Green
        TaskState.PARTIALLY_DONE -> Color(0xFFFF9800) // Orange
        TaskState.NOT_DONE -> Color(0xFFF44336) // Red
    }

    val textColor = when (state) {
        TaskState.DONE -> Color.White
        TaskState.PARTIALLY_DONE -> Color.White
        TaskState.NOT_DONE -> Color.White
    }

    val displayText = when {
        task.type == com.dailystuffapp.domain.TaskType.NUMBER_TASK -> {
            val currentValue = completion?.actualValue ?: 0
            "${task.name} ${currentValue}/${task.targetValue}"
        }
        task.type == com.dailystuffapp.domain.TaskType.TIME_TASK -> {
            val timeValue = completion?.actualValue?.takeIf { it > 0 } ?: task.targetValue
            "${task.name} ${timeValue} min"
        }
        else -> task.name
    }

    Box(
        modifier = modifier
            .background(backgroundColor, shape = MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val pressStartTime = System.currentTimeMillis()
                        tryAwaitRelease()
                        val pressDuration = System.currentTimeMillis() - pressStartTime
                        if (pressDuration >= 1000) {
                            onLongPress()
                        } else {
                            onClick()
                        }
                    }
                )
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

