# Google Drive Backup Setup Guide

This app now supports automatic daily backups to Google Drive using a **simplified shared folder approach**. This method is much easier than setting up OAuth!

## Quick Setup (Recommended)

### Step 1: Create a Google Drive Folder

1. Go to [Google Drive](https://drive.google.com)
2. Create a new folder (e.g., "DailyStuffApp_Backups")
3. Right-click the folder → **Share**
4. Choose one of these options:
   - **Easiest**: Set sharing to **"Anyone with the link can edit"** (no need to share with service account)
   - **OR**: Share it with the service account email (you'll get this email in Step 2)
   - **OR**: If you want to use your Gmail instead, share with your Gmail and use OAuth method (see "Alternative: Using Your Gmail" section below)

### Step 2: Create a Service Account (Simpler than OAuth!)

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project (or use existing)
3. Enable **Google Drive API**:
   - Go to "APIs & Services" → "Library"
   - Search for "Google Drive API"
   - Click "Enable"
4. Create a Service Account:
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "Service Account"
   - Give it a name (e.g., "DailyStuffApp Backup")
   - Click "Create and Continue"
   - Skip role assignment (click "Continue")
   - Click "Done"
5. Create a Key:
   - Click on the service account you just created
   - Go to "Keys" tab
   - Click "Add Key" → "Create new key"
   - Choose **JSON** format
   - Download the JSON file
6. (Optional) Share the folder with the service account:
   - If you set the folder to "Anyone with the link can edit" in Step 1, you can skip this step
   - Otherwise: Open the downloaded JSON file
   - Find the `client_email` field (looks like: `something@project-id.iam.gserviceaccount.com`)
   - Go back to your Google Drive folder
   - Share the folder with this email address (give it "Editor" permission)

### Step 3: Add Service Account Key to App

1. Rename the downloaded JSON file to: `service_account_key.json`
2. Place it in: `app/src/main/assets/service_account_key.json`
   - Create the `assets` folder if it doesn't exist: `app/src/main/assets/`
   - Copy the JSON file there

### Step 4: Configure Folder ID in App

You can configure the folder ID in two ways:

**Option A: Using the folder link (easiest)**

```kotlin
BackupConfigHelper.saveFolderLink(context, "https://drive.google.com/drive/folders/YOUR_FOLDER_ID")
```

**Option B: Using just the folder ID**

```kotlin
BackupConfigHelper.saveFolderId(context, "YOUR_FOLDER_ID")
```

To get the folder ID:

- Open your Google Drive folder
- Look at the URL: `https://drive.google.com/drive/folders/FOLDER_ID_HERE`
- Copy the `FOLDER_ID_HERE` part

### Step 5: Test the Backup

The backup runs automatically every day. To test it manually, you can trigger it programmatically or wait for the scheduled time.

## How It Works

- **Daily backups** are automatically scheduled when the app starts
- Backups are saved as JSON files in your specified Google Drive folder
- Each backup file is named with a timestamp: `dailystuff_backup_YYYY-MM-DD_HH-mm-ss.json`
- If a file with the same name exists, it's replaced with the new backup
- The backup includes all your tasks and task completions

## Troubleshooting

**Error: "Cannot access folder"**

- Make sure you shared the folder with the service account email
- Check that the folder ID is correct

**Error: "Failed to create Drive service"**

- Make sure `service_account_key.json` is in `app/src/main/assets/`
- Verify the JSON file is valid and not corrupted
- Make sure Google Drive API is enabled in your Google Cloud project

**Backup not running**

- Check that WorkManager has permission to run in the background
- Verify the folder ID is configured correctly
- Check Logcat for error messages (filter by "BackupWorker" or "SimpleDriveBackup")

## Alternative: Using Your Gmail (OAuth Method)

If you prefer to use your Gmail account instead of a service account:

1. **Share the folder with your Gmail:**

   - Right-click your Google Drive folder → Share
   - Add your Gmail address
   - Give it "Editor" permission

2. **Skip the service account setup** (Steps 2-3 above)

3. **Configure your Gmail account in the app:**

   ```kotlin
   // Get available Google accounts on the device
   val accounts = BackupConfigHelper.getAvailableAccounts(context)
   // Or manually set it
   BackupConfigHelper.saveAccountName(context, "your-email@gmail.com")
   ```

4. **The app will use OAuth authentication** - you may need to sign in when the backup runs

**Note:** This method requires user interaction for authentication, so it's less automated than the service account method. The service account method is recommended for fully automated daily backups.
