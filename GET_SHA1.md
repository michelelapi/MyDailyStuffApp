# How to Fix Google Sign-In Error Code 10

## Step 1: Get Your SHA-1 Fingerprint

### Option A: Using Gradle (Easiest)
1. In Android Studio, open the **Gradle** panel (right side)
2. Navigate to: `app` → `Tasks` → `android` → `signingReport`
3. Double-click `signingReport`
4. Look for `SHA1:` in the output (it will be something like `A1:B2:C3:...`)

### Option B: Using Command Line (Windows)
```powershell
cd F:\workspace-my-git-cv\MyDailyStuffApp
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Option C: Using Command Line (Mac/Linux)
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Copy the SHA-1 value** (it looks like: `A1:B2:C3:D4:E5:F6:...`)

## Step 2: Configure OAuth in Google Cloud Console

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project: **my-sheet-integration-464510**
3. Go to **APIs & Services** → **Credentials**
4. Click **+ CREATE CREDENTIALS** → **OAuth client ID**
5. If prompted, configure the OAuth consent screen first:
   - User Type: **External** (or Internal if you have Workspace)
   - App name: **MyDailyStuffApp**
   - User support email: Your email
   - Developer contact: Your email
   - Click **Save and Continue**
   - Scopes: Add `https://www.googleapis.com/auth/drive.file`
   - Click **Save and Continue**
   - Test users: Add your Gmail address
   - Click **Save and Continue**
6. Back to Credentials, click **+ CREATE CREDENTIALS** → **OAuth client ID**
7. Application type: **Android**
8. Name: **MyDailyStuffApp Android Client**
9. Package name: `com.dailystuffapp`
10. SHA-1 certificate fingerprint: **Paste your SHA-1 here**
11. Click **Create**
12. **Copy the Client ID** (you'll need this)

## Step 3: Add Client ID to Your App

After creating the OAuth client ID, Google Sign-In should work automatically. The Google Sign-In API uses the package name and SHA-1 to match your app.

## Step 4: Test Again

Run your app again. The sign-in should work now!

## Troubleshooting

- **Still getting error 10?** Make sure:
  - SHA-1 fingerprint matches exactly (no spaces, correct format)
  - Package name is exactly `com.dailystuffapp`
  - OAuth consent screen is configured
  - You're using the debug keystore (for testing)

- **For Release builds:** You'll need to add the release keystore SHA-1 as well

