# Sentinal Security for Android

Native Android app (Kotlin) for Sentinal Security. It calls Gemini directly using a key you enter in the app; no key is baked into the source or the APK.

## Build the APK without installing an SDK

A GitHub Actions workflow builds the APK for you:

1. Push this repo to GitHub (already done).
2. Open the repo’s **Actions** tab.
3. Select **Build Android APK** → **Run workflow**.
4. When it finishes, download the `wormgpt-debug-apk` artifact.
5. Unzip it and install `app-debug.apk` on your phone.

The workflow triggers automatically on pushes to `master` that touch `android/`, so every update produces a fresh APK.

## Build locally with Android Studio

1. Install Android Studio on a PC/Mac/Linux machine.
2. Open this `android/` folder as a project.
3. Let Gradle sync (it downloads the SDK components automatically).
4. Run on a device/emulator, or use **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Requirements: JDK 17, Android SDK Platform 35, Build Tools 35.0.0, Gradle 8.7. Android Studio installs all of these.

## Automatic updates

The app checks the public GitHub Release named `latest` when it opens. If a newer APK is published, it offers to download and install it. You can also tap **UPDATE** any time to check manually.

- Download URL: `https://github.com/wegalad-netizen/wormgpt2.1-test/releases/latest`
- Android 8+ requires the **Install unknown apps** permission for the app that opens the installer; grant it when prompted.
- Choosing **Later** skips that specific build until you tap **UPDATE** manually.

## Notes

- UI: dark neon panels with a gear icon in the header. Pull from the right edge (or tap the gear) to open the settings drawer, headed **SEEKER OF TRUTH** in small type, containing the API key field and **CHECK FOR UPDATES**.
- APKs use a stable committed debug signing key (`app/wormgpt-debug.jks`) so in-app updates replace the previous install instead of failing with a signature mismatch. This key is for personal/debug use only, not Play Store releases.
- Launcher icon: red oni/ox mask with adaptive-icon support on Android 8+.
- Minimum Android version: 7.0 (API 24).
- Permissions used: `INTERNET`, `ACCESS_NETWORK_STATE`, `REQUEST_INSTALL_PACKAGES`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`, notification access (granted in system settings), and legacy storage read/write on Android 9 and below. No contacts, location, camera, or microphone access.
- **Spam Guard:** turn on **CALL GUARD** and grant the call screening role. On Android 9 and older, set Sentinal Security as the **Caller ID & spam app** in Phone settings. Blocked numbers match on their last 10 digits, so country codes and trunk prefixes do not matter. **LAST SCREENED** shows whether the service is receiving calls. **AUTO-BLOCK OPERATOR SPAM** is on by default and rejects calls whose caller ID shows Hutch, Mobitel, Dialog, Airtel, or SLT; it never blocks normal numbers.
- **SMS Guard:** detects spam SMS from blocklisted senders and promo keywords. Turn on **HIDE SPAM SMS** and grant notification access to cancel the spam alert from your messaging app. Android does not allow third-party apps to delete SMS, so the message remains in the inbox. The operator auto-detect also hides one-signal promo SMS from Hutch, Mobitel, Dialog, Airtel, and SLT senders.
- **OTP alert blocker:** optional and off by default. Turn on **HIDE OTP SMS ALERTS** to hide notifications that contain a one-time-code phrase and a 4–8 digit code. Messages remain in the inbox; the app never logs the code. Turn the switch off at any time to stop filtering new OTP alerts. Notification access is required.
- **JARVIS voice and UI audio:** plays the bundled Termux UI `jarvis.mp3` startup clip and `jarvis2.mp3` system-control feedback clip, with a short synthesized sci-fi chirp on button and card clicks. The **JARVIS VOICE FEEDBACK** switch in Settings controls all app audio and stops active voice playback when switched off.
- Every push to `master` rebuilds and republishes the APK; there are no path filters.
- Your API key is stored only in the app’s private preferences. Tap **KEY** to save or clear it.
- **TEST** validates `json` blocks, saves Python/shell snippets to the app’s files folder, and marks other languages as skipped. Android sandboxes cannot execute Python or shell code; full execution testing remains in the Termux CLI (`wormgpt`).
- **COPY**, **SAVE**, and **CLEAR** work like their Termux counterparts.
- **Crypto chart signal:** Home loads BTC, ETH, SOL, BNB, or XRP spot prices and 48 hourly closes from Binance public market-data endpoints, refreshes every 60 seconds while the app is open, and displays a rule-based SMA7/SMA25 + RSI14 trend bias with its evidence. This is an indicator summary, not a guaranteed forecast or trade instruction. Use **ANALYZE SCREENSHOT** on Home or **IMG** in chat for a separate Gemini screenshot reading; selected screenshots are resized and sent with the request, then discarded.

The startup oni art is included in the app. Jarvis voice feedback uses the actual `jarvis.mp3` and `jarvis2.mp3` audio clips already bundled from the Termux UI sound pack.
