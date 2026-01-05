package com.dailystuffapp.data.backup

import android.content.Context
import android.util.Log
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.IOException
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simplified Google Drive backup service that uses a shared folder ID. Uses the same approach as
 * the Java company-service app:
 * - Service account credentials from JSON file
 * - GoogleCredentials with HttpCredentialsAdapter (modern API)
 *
 * Setup instructions:
 * 1. Create a folder in Google Drive
 * 2. Share it with "Anyone with the link can edit" or share with a service account email
 * 3. Get the folder ID from the URL: https://drive.google.com/drive/folders/FOLDER_ID_HERE
 * 4. Create a service account in Google Cloud Console and download the JSON key file
 * 5. Place the JSON file in app/src/main/assets/service_account_key.json
 * 6. Configure the folder ID in BackupConfigHelper
 */
class SimpleDriveBackupService(private val context: Context) {
    private val TAG = "SimpleDriveBackup"
    private val SERVICE_ACCOUNT_KEY_FILE = "service_account_key.json"
    private val APPLICATION_NAME = "DailyStuffApp"

    private fun getDriveService(): Drive? {
        return try {
            // Load service account credentials from assets (same approach as Java app)
            val inputStream = context.assets.open(SERVICE_ACCOUNT_KEY_FILE)
            val credentials =
                    GoogleCredentials.fromStream(inputStream)
                            .createScoped(Collections.singletonList(DriveScopes.DRIVE_FILE))

            // Use HttpCredentialsAdapter to wrap credentials (same as Java app)
            val credentialAdapter = HttpCredentialsAdapter(credentials)

            Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credentialAdapter)
                    .setApplicationName(APPLICATION_NAME)
                    .build()
        } catch (e: IOException) {
            Log.e(
                    TAG,
                    "Error loading service account key. Make sure service_account_key.json is in assets folder.",
                    e
            )
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Drive service", e)
            null
        }
    }

    suspend fun uploadBackupFile(backupFile: java.io.File, folderId: String): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val driveService =
                            getDriveService()
                                    ?: return@withContext Result.failure(
                                            Exception(
                                                    "Failed to create Drive service. Check service account key file."
                                            )
                                    )

                    Log.d(TAG, "Attempting to upload to folder ID: $folderId")
                    Log.d(TAG, "Uploading file: ${backupFile.name} to folder: $folderId")

                    // Check if file with same name exists and delete it (same approach as Java app)
                    val existingFileId = findExistingFile(driveService, folderId, backupFile.name)
                    if (existingFileId != null) {
                        Log.d(
                                TAG,
                                "Found existing file with same name, deleting it first: $existingFileId"
                        )
                        try {
                            driveService
                                    .files()
                                    .delete(existingFileId)
                                    .setSupportsAllDrives(true)
                                    .execute()
                            Log.d(TAG, "Deleted existing file successfully")
                        } catch (e: Exception) {
                            Log.w(
                                    TAG,
                                    "Failed to delete existing file, will try to upload anyway",
                                    e
                            )
                        }
                    }

                    // Upload the file (same approach as Java app - create new file)
                    val fileMetadata =
                            File().apply {
                                name = backupFile.name
                                parents = Collections.singletonList(folderId)
                            }

                    val mediaContent =
                            com.google.api.client.http.FileContent("application/json", backupFile)
                    val uploadedFile =
                            driveService
                                    .files()
                                    .create(fileMetadata, mediaContent)
                                    .setFields("id, name, webViewLink")
                                    .setSupportsAllDrives(true)
                                    .execute()

                    Log.d(
                            TAG,
                            "Backup uploaded successfully: ${uploadedFile.name} (ID: ${uploadedFile.id})"
                    )
                    Result.success("Backup uploaded successfully: ${uploadedFile.name}")
                } catch (e: IOException) {
                    Log.e(TAG, "IO error uploading backup", e)
                    Result.failure(e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error uploading backup", e)
                    Result.failure(e)
                }
            }

    private fun findExistingFile(driveService: Drive, folderId: String, fileName: String): String? {
        return try {
            val query = "name='$fileName' and '$folderId' in parents and trashed=false"
            val result: FileList =
                    driveService
                            .files()
                            .list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("files(id, name)")
                            .setSupportsAllDrives(true)
                            .setIncludeItemsFromAllDrives(true)
                            .execute()

            if (result.files.isNotEmpty()) {
                result.files[0].id
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding existing file", e)
            null
        }
    }
}
