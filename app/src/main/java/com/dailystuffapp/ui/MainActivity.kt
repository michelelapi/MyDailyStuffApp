package com.dailystuffapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.dailystuffapp.data.backup.DatabaseBackup
import com.dailystuffapp.data.database.AppDatabase
import com.dailystuffapp.data.repository.TaskRepository
import com.dailystuffapp.navigation.NavGraph
import com.dailystuffapp.ui.viewmodel.ChartViewModel
import com.dailystuffapp.ui.viewmodel.ChartViewModelFactory
import com.dailystuffapp.ui.viewmodel.CreateTaskViewModel
import com.dailystuffapp.ui.viewmodel.CreateTaskViewModelFactory
import com.dailystuffapp.ui.viewmodel.EditTasksViewModel
import com.dailystuffapp.ui.viewmodel.EditTasksViewModelFactory
import com.dailystuffapp.ui.viewmodel.MainViewModel
import com.dailystuffapp.ui.viewmodel.MainViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Register for Google Sign-In result
    private val signInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                    android.util.Log.d("MainActivity", "✅ Sign-in successful: ${account.email}")
                    android.util.Log.d("MainActivity", "Granted scopes: ${account.grantedScopes}")

                    // Check if Drive scope was granted
                    val hasDriveScope =
                            account.grantedScopes?.any { it.toString().contains("drive") } == true

                    if (hasDriveScope) {
                        android.util.Log.d(
                                "MainActivity",
                                "✅ Drive scope granted, starting backup..."
                        )
                        performBackup()
                    } else {
                        android.util.Log.e(
                                "MainActivity",
                                "❌ Drive scope NOT granted! Please sign in again and grant Drive access."
                        )
                    }
                } catch (e: ApiException) {
                    android.util.Log.e("MainActivity", "❌ Sign-in failed", e)
                    android.util.Log.e(
                            "MainActivity",
                            "Error code: ${e.statusCode}, Message: ${e.message}"
                    )
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = TaskRepository(database)

        // Check if already signed in, if not, trigger sign-in
        if (!DatabaseBackup.isSignedIn(this)) {
            android.util.Log.d("MainActivity", "Not signed in, triggering Google Sign-In...")
            val signInIntent = DatabaseBackup.getSignInIntent(this)
            signInLauncher.launch(signInIntent)
        } else {
            android.util.Log.d("MainActivity", "Already signed in, performing backup...")
            performBackup()
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val mainViewModel: MainViewModel =
                            viewModel(factory = MainViewModelFactory(repository))
                    val createTaskViewModel: CreateTaskViewModel =
                            viewModel(factory = CreateTaskViewModelFactory(repository))
                    val chartViewModel: ChartViewModel =
                            viewModel(factory = ChartViewModelFactory(repository))
                    val editTasksViewModel: EditTasksViewModel =
                            viewModel(factory = EditTasksViewModelFactory(repository))

                    NavGraph(
                            navController = navController,
                            mainViewModel = mainViewModel,
                            createTaskViewModel = createTaskViewModel,
                            chartViewModel = chartViewModel,
                            editTasksViewModel = editTasksViewModel
                    )
                }
            }
        }
    }

    private fun performBackup() {
        // Check if backup was already done today
        if (DatabaseBackup.wasBackupDoneToday(this)) {
            android.util.Log.d("MainActivity", "⏭️ Backup already performed today. Skipping...")
            return
        }

        // Perform backup on app start
        CoroutineScope(Dispatchers.IO).launch {
            android.util.Log.d("MainActivity", "Starting backup process...")
            val backup = DatabaseBackup(this@MainActivity)
            val result = backup.backupToGoogleDrive()
            result
                    .onSuccess {
                        android.util.Log.d(
                                "MainActivity",
                                "✅ Backup completed successfully on app start"
                        )
                    }
                    .onFailure { exception ->
                        android.util.Log.e(
                                "MainActivity",
                                "❌ Backup failed on app start",
                                exception
                        )
                        android.util.Log.e("MainActivity", "Error message: ${exception.message}")
                        exception.printStackTrace()
                    }
        }
    }
}
