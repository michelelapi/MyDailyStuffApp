package com.dailystuffapp.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.dailystuffapp.data.database.AppDatabase
import com.dailystuffapp.data.database.Task
import com.dailystuffapp.data.database.TaskCompletion
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseBackup(private val context: Context) {
    private val TAG = "DatabaseBackup"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // Google Drive folder ID from the URL
    private val driveFolderId = "1EXMg1mReaYMamOcLLRIuqZ1aviXMjYX6"

    // SharedPreferences key for tracking last backup date
    private val prefs: SharedPreferences =
            context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
    private val LAST_BACKUP_DATE_KEY = "last_backup_date"

    companion object {
        /**
         * Gets the Google Sign-In intent for signing in with Drive scope. Call this from an
         * Activity and use startActivityForResult() or registerForActivityResult() to handle the
         * sign-in.
         */
        fun getSignInIntent(context: Context): android.content.Intent {
            val signInOptions =
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(Scope(DriveScopes.DRIVE))
                            .requestEmail()
                            .build()
            val googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
            return googleSignInClient.signInIntent
        }

        /** Checks if a Google account is signed in with Drive scope */
        fun isSignedIn(context: Context): Boolean {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            return account != null &&
                    account.grantedScopes?.any { it.toString().contains(DriveScopes.DRIVE) } == true
        }

        /** Checks if a backup was already performed today */
        fun wasBackupDoneToday(context: Context): Boolean {
            val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val lastBackupDate = prefs.getString("last_backup_date", null)

            if (lastBackupDate == null) {
                return false
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            return lastBackupDate == today
        }
    }

    /** Creates a JSON backup of all database data and uploads it to Google Drive */
    suspend fun backupToGoogleDrive(): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    // Check if backup was already done today
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val lastBackupDate = prefs.getString(LAST_BACKUP_DATE_KEY, null)

                    if (lastBackupDate == today) {
                        Log.d(TAG, "Backup already performed today ($today). Skipping...")
                        return@withContext Result.success(Unit)
                    }

                    Log.d(
                            TAG,
                            "Starting database backup... (Last backup: ${lastBackupDate ?: "never"})"
                    )

                    // Get database instance
                    val database = AppDatabase.getDatabase(context)

                    // Fetch all data from database
                    val tasks = database.taskDao().getAllTasksList()
                    val completions = database.taskCompletionDao().getAllCompletions()

                    Log.d(TAG, "Fetched ${tasks.size} tasks and ${completions.size} completions")

                    // Create backup data structure
                    val backupData =
                            BackupData(
                                    timestamp = System.currentTimeMillis(),
                                    date =
                                            SimpleDateFormat(
                                                            "yyyy-MM-dd HH:mm:ss",
                                                            Locale.getDefault()
                                                    )
                                                    .format(Date()),
                                    tasks = tasks,
                                    taskCompletions = completions
                            )

                    // Convert to JSON
                    val jsonString = gson.toJson(backupData)
                    Log.d(TAG, "Created JSON backup (${jsonString.length} bytes)")

                    // Upload to Google Drive
                    uploadToDrive(jsonString)

                    // Save today's date as last backup date
                    prefs.edit().putString(LAST_BACKUP_DATE_KEY, today).apply()

                    Log.d(TAG, "Backup completed successfully. Saved backup date: $today")
                    Result.success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "Backup failed", e)
                    Result.failure(e)
                }
            }

    /** Uploads the JSON backup to Google Drive */
    private suspend fun uploadToDrive(jsonContent: String) =
            withContext(Dispatchers.IO) {
                try {
                    // Configure Google Sign-In to request Drive scope
                    val signInOptions =
                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestScopes(Scope(DriveScopes.DRIVE))
                                    .requestEmail()
                                    .build()

                    // Try to get the last signed-in account (silent sign-in)
                    val googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
                    val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

                    if (account == null) {
                        throw IllegalStateException(
                                "No Google account signed in.\n\n" +
                                        "To fix this:\n" +
                                        "1. The app needs Google Sign-In to access Drive\n" +
                                        "2. You need to sign in with Google first\n" +
                                        "3. Call DatabaseBackup.getSignInIntent() from your Activity\n" +
                                        "4. After signing in, the backup will work automatically"
                        )
                    }

                    Log.d(TAG, "Using Google account: ${account.email}")

                    // Check if account has Drive scope
                    val hasDriveScope =
                            account.grantedScopes?.any {
                                it.toString().contains(DriveScopes.DRIVE)
                            } == true

                    if (!hasDriveScope) {
                        throw IllegalStateException(
                                "Drive permission not granted. " +
                                        "Please sign in again and grant Drive access when prompted."
                        )
                    }

                    // Use GoogleAccountCredential with the signed-in account
                    val credential =
                            GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE))
                    credential.selectedAccountName = account.email

                    Log.d(TAG, "Credential created for account: ${credential.selectedAccountName}")

                    // Build Drive service with credential
                    val driveService = buildDriveService(credential)
                    Log.d(TAG, "Drive service built successfully")

                    Log.d(TAG, "Attempting to upload file to folder ID: $driveFolderId")
                    uploadFile(driveService, jsonContent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload to Google Drive", e)
                    throw e
                }
            }

    /** Builds Drive service with credentials */
    private fun buildDriveService(
            credential:
                    com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
    ): Drive {
        return Drive.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential
                )
                .setApplicationName("MyDailyStuffApp")
                .build()
    }

    /** Uploads file to Drive */
    private suspend fun uploadFile(driveService: Drive, jsonContent: String) {
        try {
            // Create filename with timestamp
            val timestamp =
                    SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val filename = "daily_stuff_backup_$timestamp.json"
            Log.d(TAG, "Creating file: $filename")

            // Create file metadata
            val fileMetadata =
                    File().apply {
                        name = filename
                        parents = listOf(driveFolderId)
                    }
            Log.d(TAG, "File metadata created. Parent folder: $driveFolderId")

            // Convert JSON string to InputStream
            val contentStream: InputStream =
                    ByteArrayInputStream(jsonContent.toByteArray(Charsets.UTF_8))
            Log.d(TAG, "Content stream created (${jsonContent.length} bytes)")

            // Upload file
            val mediaContent =
                    com.google.api.client.http.InputStreamContent("application/json", contentStream)

            Log.d(TAG, "Starting file upload...")
            val uploadedFile =
                    driveService
                            .files()
                            .create(fileMetadata, mediaContent)
                            .setFields("id, name, parents")
                            .setSupportsAllDrives(true)
                            .execute()

            Log.d(TAG, "✅ File uploaded successfully!")
            Log.d(TAG, "   File name: ${uploadedFile.name}")
            Log.d(TAG, "   File ID: ${uploadedFile.id}")
            Log.d(TAG, "   File parents: ${uploadedFile.parents}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload file to Drive", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /** Data class representing the backup structure */
    data class BackupData(
            val timestamp: Long,
            val date: String,
            val tasks: List<Task>,
            val taskCompletions: List<TaskCompletion>
    )
}
