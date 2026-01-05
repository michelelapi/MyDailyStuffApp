package com.dailystuffapp.data.backup

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import java.io.IOException
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveBackupService(private val context: Context) {
    private val TAG = "GoogleDriveBackup"
    private val SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE)
    private val FOLDER_NAME = "DailyStuffApp_Backups"

    private fun getDriveService(accountName: String): Drive? {
        Log.e(TAG, "=== getDriveService START: accountName=$accountName ===")
        return try {
            if (accountName.isBlank()) {
                Log.e(TAG, "Account name is empty or null")
                return null
            }

            Log.e(TAG, "Creating GoogleAccountCredential...")
            val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)

            // Try AccountManager first (more reliable than credential.allAccounts)
            Log.e(TAG, "Getting accounts using AccountManager...")
            val accountManager = AccountManager.get(context)
            val accountManagerAccounts = accountManager.getAccountsByType("com.google")
            Log.e(TAG, "AccountManager found ${accountManagerAccounts.size} Google accounts")

            // Also try credential.allAccounts
            val credentialAccounts = credential.allAccounts
            Log.e(TAG, "GoogleAccountCredential found ${credentialAccounts.size} accounts")

            // Combine both sources, prefer AccountManager
            val availableAccounts =
                    if (accountManagerAccounts.isNotEmpty()) {
                        accountManagerAccounts.toList()
                    } else {
                        credentialAccounts.toList()
                    }

            Log.e(TAG, "Total available accounts: ${availableAccounts.size}")
            availableAccounts.forEachIndexed { index, account ->
                Log.e(TAG, "Account[$index]: name='${account.name}', type='${account.type}'")
            }

            // Try to find account by name (case-insensitive)
            val accountExists =
                    availableAccounts.any { it.name.equals(accountName, ignoreCase = true) }

            if (!accountExists) {
                Log.e(TAG, "Account '$accountName' NOT FOUND in available accounts!")

                // If there are any Google accounts, use the first one automatically
                val googleAccounts =
                        availableAccounts.filter {
                            it.type.contains("google", ignoreCase = true) ||
                                    it.name.contains("@gmail.com", ignoreCase = true) ||
                                    it.name.contains("@googlemail.com", ignoreCase = true)
                        }

                Log.e(TAG, "Found ${googleAccounts.size} Google accounts after filtering")

                if (googleAccounts.isNotEmpty()) {
                    val fallbackAccount = googleAccounts[0].name
                    Log.e(TAG, "FALLBACK: Using first available Google account: '$fallbackAccount'")
                    credential.selectedAccountName = fallbackAccount
                } else {
                    // No accounts detected in AccountManager, but account might be authenticated
                    // via Google Play Services
                    // Try using the account name directly - GoogleAccountCredential might be able
                    // to authenticate
                    // through Google Play Services even if AccountManager doesn't see it
                    Log.w(
                            TAG,
                            "No accounts found in AccountManager, but trying to use account name: '$accountName'"
                    )
                    Log.w(
                            TAG,
                            "Note: Account must be signed in to Google Play Services or added in Settings > Accounts"
                    )
                    credential.selectedAccountName = accountName
                    // Don't return null - let it try and fail gracefully if authentication doesn't
                    // work
                }
            } else {
                // Find the exact account (case-insensitive match)
                val matchingAccount =
                        availableAccounts.firstOrNull {
                            it.name.equals(accountName, ignoreCase = true)
                        }

                credential.selectedAccountName = matchingAccount?.name ?: accountName
                Log.e(TAG, "Using configured account: ${credential.selectedAccountName}")
            }

            // Validate that we have a valid account set
            val finalAccount = credential.selectedAccountName
            if (finalAccount.isNullOrBlank()) {
                Log.e(
                        TAG,
                        "ERROR: Failed to set account name. Cannot proceed without a valid account."
                )
                return null
            }

            Log.e(TAG, "Final selected account: $finalAccount")

            Log.e(TAG, "Building Drive service...")
            val driveService =
                    Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                            .setApplicationName("DailyStuffApp")
                            .build()

            Log.e(TAG, "=== getDriveService SUCCESS ===")
            driveService
        } catch (e: Exception) {
            Log.e(TAG, "=== getDriveService ERROR ===", e)
            e.printStackTrace()
            null
        }
    }

    suspend fun uploadBackupFile(
            backupFile: java.io.File,
            accountName: String,
            folderId: String? = null
    ): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    if (accountName.isBlank()) {
                        return@withContext Result.failure(
                                Exception(
                                        "Account name is empty. Please configure a Google account."
                                )
                        )
                    }

                    val driveService =
                            getDriveService(accountName)
                                    ?: return@withContext Result.failure(
                                            Exception(
                                                    "Failed to create Drive service. Account '$accountName' may not exist on device."
                                            )
                                    )

                    // Use provided folder ID or find/create backup folder
                    val targetFolderId =
                            folderId
                                    ?: findOrCreateBackupFolder(driveService)
                                            ?: return@withContext Result.failure(
                                            Exception("Failed to get backup folder ID")
                                    )

                    // Check if file with same name exists and delete it
                    val existingFileId =
                            findExistingFile(driveService, targetFolderId, backupFile.name)
                    existingFileId?.let {
                        driveService.files().delete(it).setSupportsAllDrives(true).execute()
                    }

                    // Upload the file
                    val fileMetadata =
                            File().apply {
                                name = backupFile.name
                                parents = Collections.singletonList(targetFolderId)
                            }

                    val mediaContent = FileContent("application/json", backupFile)
                    val uploadedFile =
                            driveService
                                    .files()
                                    .create(fileMetadata, mediaContent)
                                    .setFields("id, name, webViewLink")
                                    .setSupportsAllDrives(true)
                                    .execute()

                    Log.d(TAG, "Backup uploaded successfully: ${uploadedFile.name}")
                    Result.success("Backup uploaded successfully: ${uploadedFile.name}")
                } catch (e: UserRecoverableAuthException) {
                    Log.e(TAG, "User recoverable auth exception", e)
                    Result.failure(e)
                } catch (e: IllegalArgumentException) {
                    // This usually means the account doesn't exist in the system
                    if (e.message?.contains("the name must not be empty") == true) {
                        val errorMsg =
                                "Google account '$accountName' is not added to the device. " +
                                        "Please add it in Settings > Accounts > Add Account > Google. " +
                                        "Note: Signing in to Google services is different from adding the account to the device."
                        Log.e(TAG, errorMsg, e)
                        Result.failure(Exception(errorMsg, e))
                    } else {
                        Log.e(TAG, "IllegalArgumentException during backup", e)
                        Result.failure(e)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "IO error uploading backup", e)
                    Result.failure(e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error uploading backup", e)
                    Result.failure(e)
                }
            }

    private fun findOrCreateBackupFolder(driveService: Drive): String? {
        return try {
            // Search for existing folder
            val query =
                    "name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
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
                // Create new folder
                val folderMetadata =
                        File().apply {
                            name = FOLDER_NAME
                            mimeType = "application/vnd.google-apps.folder"
                        }
                val folder =
                        driveService
                                .files()
                                .create(folderMetadata)
                                .setFields("id")
                                .setSupportsAllDrives(true)
                                .execute()
                folder.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding/creating folder", e)
            null
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

    fun getAccounts(): List<Account> {
        return try {
            val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
            credential.allAccounts.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting accounts", e)
            emptyList()
        }
    }
}
