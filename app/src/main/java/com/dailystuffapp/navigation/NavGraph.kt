package com.dailystuffapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dailystuffapp.ui.screens.ChartScreen
import com.dailystuffapp.ui.screens.CreateTaskScreen
import com.dailystuffapp.ui.screens.EditTasksScreen
import com.dailystuffapp.ui.screens.MainScreen
import com.dailystuffapp.ui.viewmodel.ChartViewModel
import com.dailystuffapp.ui.viewmodel.CreateTaskViewModel
import com.dailystuffapp.ui.viewmodel.EditTasksViewModel
import com.dailystuffapp.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object CreateTask : Screen("create_task")
    object Chart : Screen("chart")
    object EditTasks : Screen("edit_tasks")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    createTaskViewModel: CreateTaskViewModel,
    chartViewModel: ChartViewModel,
    editTasksViewModel: EditTasksViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = mainViewModel,
                onNavigateToCreateTask = {
                    navController.navigate(Screen.CreateTask.route)
                },
                onNavigateToChart = {
                    navController.navigate(Screen.Chart.route)
                },
                onNavigateToEditTasks = {
                    navController.navigate(Screen.EditTasks.route)
                }
            )
        }

        composable(Screen.CreateTask.route) {
            CreateTaskScreen(
                viewModel = createTaskViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                    createTaskViewModel.reset()
                }
            )
        }

        composable(Screen.Chart.route) {
            ChartScreen(
                viewModel = chartViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.EditTasks.route) {
            EditTasksScreen(
                viewModel = editTasksViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

