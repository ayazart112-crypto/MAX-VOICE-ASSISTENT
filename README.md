# MAX - Voice Assistant by Allen

A native Android voice assistant built in Kotlin with 25+ voice commands and full phone control.

## Features / Commands

| Category | Commands |
|----------|----------|
| Time & Date | "What time is it", "What's today's date", "What day is it" |
| Calls | "Call [name]", "Redial" |
| Messages | "Send WhatsApp to [name]", "Send SMS", "Read messages" |
| Apps | "Open YouTube", "Open Instagram", "Open WhatsApp", "Open Settings", etc. |
| Music | "Play [song name]", "Search song [name]" |
| YouTube | "Play [video] on YouTube" |
| Flashlight | "Flashlight on / off", "Turn on torch" |
| Volume | "Volume up / down", "Mute", "Unmute", "Silent" |
| Alarm | "Set alarm for 7 AM", "Show alarms" |
| Timer | "Set timer for 5 minutes" |
| WiFi | "WiFi on / off" |
| Bluetooth | "Bluetooth on / off" |
| Navigation | "Navigate to [place]", "Take me to [place]" |
| Camera | "Take photo", "Take selfie" |
| Weather | "Weather" |
| Search | "Search [query]", "Google [query]" |
| Battery | "Battery status" |
| Settings | "Brightness", "Airplane mode" |
| Fun | "Tell me a joke", "How are you", "Who are you" |
| Help | "What can you do", "Help" |

---

## How to Build & Install

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Java 8 or higher
- Android device with API 24+ (Android 7.0+)

### Option 1: Android Studio (Recommended)
1. Open Android Studio
2. Click **File > Open** and select the `MAX` folder
3. Wait for Gradle sync to complete
4. Connect your Android phone via USB with USB Debugging enabled
5. Click the **Run ▶** button

### Option 2: Command Line
```bash
# In the MAX project directory
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk

# Install directly to connected phone:
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Enable USB Debugging on your phone:
1. Go to **Settings > About Phone**
2. Tap **Build Number** 7 times
3. Go back to **Settings > Developer Options**
4. Enable **USB Debugging**

---

## Project Structure

```
MAX/
├── app/
│   ├── src/main/
│   │   ├── java/com/allen/max/
│   │   │   ├── MainActivity.kt          # Main UI + voice input/output
│   │   │   ├── commands/
│   │   │   │   └── CommandProcessor.kt  # All 25+ voice commands
│   │   │   ├── services/
│   │   │   │   └── MAXService.kt        # Background service
│   │   │   └── utils/
│   │   │       └── Helpers.kt           # Date, contacts, battery helpers
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml # UI layout
│   │   │   ├── drawable/                # Icons & button styles
│   │   │   └── values/                  # Colors, strings, themes
│   │   └── AndroidManifest.xml         # Permissions & components
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## Permissions Required
MAX will ask for these permissions on first launch:
- Microphone (voice input)
- Phone (make calls)
- Contacts (call by name)
- SMS (send messages)
- Camera (photos/flashlight)
- Location (navigation)

Grant all permissions for full functionality.
