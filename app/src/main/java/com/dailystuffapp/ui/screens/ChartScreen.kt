package com.dailystuffapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailystuffapp.ui.viewmodel.ChartViewModel
import com.dailystuffapp.ui.viewmodel.DayCompletionData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(viewModel: ChartViewModel, onNavigateBack: () -> Unit) {
    val completionData by viewModel.completionData.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    var showTaskDropdown by remember { mutableStateOf(false) }

    // Refresh data when screen is displayed
    LaunchedEffect(Unit) { viewModel.refresh() }

    // Refresh when tasks list changes (might indicate new completions)
    LaunchedEffect(allTasks.size, allTasks.map { it.id }.toSet()) { viewModel.refresh() }

    val weeksData =
            remember(completionData, selectedTaskId) { viewModel.getCompletionDataForYear() }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Activity Chart") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // Task filter dropdown
            Box {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable { showTaskDropdown = true }
                                        .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            modifier = Modifier.size(20.dp)
                    )
                    Text(text = "Filter by:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                            text =
                                    selectedTaskId?.let { taskId ->
                                        allTasks.find { it.id == taskId }?.name ?: "All Tasks"
                                    }
                                            ?: "All Tasks",
                            style = MaterialTheme.typography.bodyMedium
                    )
                }
                DropdownMenu(
                        expanded = showTaskDropdown,
                        onDismissRequest = { showTaskDropdown = false }
                ) {
                    DropdownMenuItem(
                            text = { Text("All Tasks") },
                            onClick = {
                                viewModel.setSelectedTask(null)
                                showTaskDropdown = false
                            }
                    )
                    allTasks.forEach { task ->
                        DropdownMenuItem(
                                text = { Text(task.name) },
                                onClick = {
                                    viewModel.setSelectedTask(task.id)
                                    showTaskDropdown = false
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart - organized by years
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                val years = viewModel.getYearsInData()

                years.forEach { year ->
                    // Year header
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Get weeks for this year - keep full week structure but only show days from
                    // this year
                    val yearWeeks =
                            weeksData.filter { week ->
                                // Include weeks that have at least one day from this year
                                week.any { it.date.year == year }
                            }

                    if (yearWeeks.isNotEmpty()) {
                        // Make chart horizontally scrollable to fit all months
                        val horizontalScrollState = rememberScrollState()

                        Column {
                            // Group weeks by month and organize into pairs (2 weeks per column)
                            val monthFormatter =
                                    DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
                            val weeksByMonth =
                                    mutableMapOf<Int, MutableList<List<DayCompletionData>>>()

                            yearWeeks.forEach { week: List<DayCompletionData> ->
                                if (week.isNotEmpty()) {
                                    // Find the first day of this year in the week to determine the
                                    // month
                                    val firstDayOfYear = week.firstOrNull { it.date.year == year }
                                    if (firstDayOfYear != null) {
                                        val month = firstDayOfYear.date.monthValue
                                        weeksByMonth.getOrPut(month) { mutableListOf() }.add(week)
                                    }
                                }
                            }

                            // Month labels row for this year (just first letter)
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .horizontalScroll(horizontalScrollState),
                                    horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.width(16.dp)) // Space for week labels
                                weeksByMonth.keys.sorted().forEach { month ->
                                    val monthWeeks = weeksByMonth[month] ?: emptyList()
                                    // Calculate how many columns this month needs (2 weeks per
                                    // column)
                                    val numColumns = (monthWeeks.size + 1) / 2
                                    // Show month label centered over all columns for this month
                                    if (numColumns > 0) {
                                        // Create a date for the first day of this month in the
                                        // current year
                                        val monthDate = LocalDate.of(year, month, 1)
                                        // Each column is 8.dp (box) + 1.dp (spacing after) =
                                        // 9.dp
                                        // Total width includes all columns and their spacers
                                        val monthWidth = 9.dp * numColumns
                                        Box(
                                                modifier = Modifier.width(monthWidth),
                                                contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                    text =
                                                            monthDate
                                                                    .format(monthFormatter)
                                                                    .first()
                                                                    .toString(),
                                                    style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Chart grid for this year (horizontally scrollable)
                            // Organize weeks: 2 weeks per column vertically
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .horizontalScroll(horizontalScrollState),
                                    horizontalArrangement = Arrangement.Start
                            ) {
                                // Week labels column (1 and 2)
                                Column(
                                        modifier = Modifier.width(16.dp),
                                        verticalArrangement = Arrangement.Top
                                ) {
                                    // Label "1" aligned with first week
                                    // First week: 7 days * 8.dp + 6 spacings * 1.dp = 56.dp + 6.dp
                                    // = 62.dp
                                    Text(
                                            text = "1",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                    // Space for first week (7 boxes + 6 spacings = 62.dp)
                                    Spacer(modifier = Modifier.height(60.dp))
                                    // Label "2" aligned with second week
                                    Text(
                                            text = "2",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                weeksByMonth.keys.sorted().forEachIndexed { monthIndex, month ->
                                    val monthWeeks = weeksByMonth[month] ?: emptyList()
                                    // Alternate month background color
                                    val monthBackgroundColor =
                                    // if (monthIndex % 2 == 0) {
                                    //     Color(0xFF1E1E1E) // Slightly lighter gray
                                    // } else {
                                    Color(0xFF1A1A1A) // Slightly darker gray
                                    // }

                                    // Group weeks into pairs (2 weeks per column)
                                    monthWeeks.chunked(2).forEach {
                                            weekPair: List<List<DayCompletionData>> ->
                                        Column(
                                                verticalArrangement = Arrangement.spacedBy(1.dp),
                                                modifier = Modifier.background(monthBackgroundColor)
                                        ) {
                                            // Display up to 2 weeks in this column
                                            weekPair.forEachIndexed {
                                                    weekIndex,
                                                    week: List<DayCompletionData> ->
                                                // Alternate week background color
                                                // val weekBackgroundColor =
                                                //         if (weekIndex % 2 == 0) {
                                                //             Color(
                                                //                     0xFF1E1E1E
                                                //             ) // Slightly lighter gray
                                                //         } else {
                                                //             Color(
                                                //                     0xFF1A1A1A
                                                //             ) // Slightly darker gray
                                                //         }
                                                Column(
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(1.dp),
                                                        // modifier =
                                                        //         Modifier.background(
                                                        //                 weekBackgroundColor
                                                        //         )
                                                        ) {
                                                    week.forEach { dayData: DayCompletionData ->
                                                        // Only show days from this year
                                                        if (dayData.date.year == year) {
                                                            val color =
                                                                    getColorForPercentage(
                                                                            dayData.completionPercentage
                                                                    )
                                                            Box(
                                                                    modifier =
                                                                            Modifier.size(8.dp)
                                                                                    .background(
                                                                                            color,
                                                                                            shape =
                                                                                                    RectangleShape
                                                                                    )
                                                            )
                                                        } else {
                                                            // Empty space for days not in this year
                                                            Spacer(modifier = Modifier.size(8.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorBox(color: Color) {
    Box(modifier = Modifier.size(10.dp).background(color, shape = RectangleShape))
}

fun getColorForPercentage(percentage: Float): Color {

    return when {
        percentage == 0f -> Color(0xFF111111) // White
        percentage < 0.2f -> Color(0xFF9BE9A8) // Light green (1-33%)
        percentage < 0.5f -> Color(0xFF40C463) // Medium green (34-66%)
        percentage < 1f -> Color(0xFF30A14E) // Dark green (67-99%)
        else -> Color(0xFF216E39) // Very dark green (100%)
    }
}
