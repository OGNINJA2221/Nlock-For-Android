# N Lock - Secure App Lock for Android

A modern Android application locker built with Jetpack Compose, Material Design 3, and biometric security. Protect your apps using encrypted PIN, Pattern, or Knock Lock sequences with intruder selfie capture and privacy stealth features.

---

## 📱 How to Download the Android Application

You can get the downloadable Android APK (`.apk`) using any of the following methods:

### Option 1: Download from GitHub Releases (Recommended)
1. Go to the **Releases** section on the right side of this GitHub repository page (or navigate to `https://github.com/<your-username>/<repo-name>/releases`).
2. Under the latest release **Assets**, click on **`N-Lock-release.apk`** to download it directly to your phone or computer.

### Option 2: Download from GitHub Actions (Continuous Integration)
Every merge to `main` or pushed tag triggers `.github/workflows/android.yml` to automatically compile a signed Release APK:
1. Click on the **Actions** tab at the top of the GitHub repository.
2. Select the latest **"Android Release CI"** workflow run.
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Click **`N-Lock-Release-APK`** to download the generated release APK.

### Option 3: Download Directly from Google AI Studio
1. Open the project in [Google AI Studio Build](https://ai.studio/build).
2. Open the **Settings** or project menu in the top-right corner.
3. Select **Download APK** or **Export Project** to download the installer directly to your device.

---

## 📲 How to Install the APK on Your Android Device

1. Transfer or download the `.apk` file to your Android phone or tablet.
2. Tap the downloaded file in your browser or file manager.
3. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Tap **Settings**.
   - Enable **Allow from this source**.
4. Tap **Install** to complete the installation.
5. Launch **N Lock** and set up your preferred security lock (PIN, Pattern, or Knock Lock).

---

## 🔒 Features

- **Multi-Method Security**:
  - **PIN Lock**: Custom 4-digit code with frosted glass numeric keypad.
  - **Pattern Lock**: Connect-the-dots grid with visual trace animations and error feedback.
  - **Knock Lock**: 4-quadrant + center sequence with tactile feedback.
  - **Biometrics**: Fingerprint and Face Unlock support via Android BiometricPrompt.
- **Hardware-Backed Encryption**: All credentials encrypted using AES-GCM via the Android Keystore system.
- **Intruder Selfie Capture**: Automatically snaps a silent photo of unauthorized access attempts.
- **Reliable App Locking**: Seamlessly shields protected apps and resumes them immediately upon verification without disrupting task flow.

---

## 🛠️ Building Locally

To build the project locally with Android Studio or the command line:

```bash
# Clone the repository
git clone https://github.com/<your-username>/<repo-name>.git
cd <repo-name>

# Build the Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```
