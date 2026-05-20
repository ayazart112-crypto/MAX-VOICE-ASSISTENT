package com.allen.max.commands

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import com.allen.max.utils.ContactHelper
import com.allen.max.utils.DateTimeHelper
import com.allen.max.utils.BatteryHelper
import java.util.Calendar

class CommandProcessor(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var flashlightOn = false

    fun process(command: String): String {
        return when {
            // === TIME & DATE ===
            command.contains("time") -> DateTimeHelper.getTime()
            command.contains("date") || command.contains("today") -> DateTimeHelper.getDate()
            command.contains("day") -> DateTimeHelper.getDay()

            // === CALLS ===
            command.contains("call") -> handleCall(command)
            command.contains("redial") || command.contains("call back") -> handleRedial()

            // === MESSAGES ===
            command.contains("send") && (command.contains("message") || command.contains("whatsapp") || command.contains("sms")) -> handleMessage(command)
            command.contains("read") && command.contains("message") -> "Opening messages for you."

            // === FLASHLIGHT ===
            command.contains("flashlight") || command.contains("torch") -> toggleFlashlight(command)

            // === VOLUME ===
            command.contains("volume up") || command.contains("increase volume") -> adjustVolume(true)
            command.contains("volume down") || command.contains("decrease volume") -> adjustVolume(false)
            command.contains("mute") -> mutePhone()
            command.contains("unmute") || command.contains("silent off") -> unmutePhone()
            command.contains("silent") -> setSilentMode()

            // === APPS ===
            command.contains("open") || command.contains("launch") -> handleOpenApp(command)

            // === YOUTUBE & MUSIC ===
            command.contains("play") && command.contains("youtube") -> handleYouTubePlay(command)
            command.contains("play") || command.contains("search song") || command.contains("music") -> handleMusicPlay(command)

            // === BROWSER / SEARCH ===
            command.contains("search") || command.contains("google") -> handleWebSearch(command)

            // === ALARM ===
            command.contains("alarm") || command.contains("wake me") -> handleAlarm(command)
            command.contains("timer") -> handleTimer(command)

            // === SETTINGS ===
            command.contains("wifi on") || command.contains("turn on wifi") -> handleWifi(true)
            command.contains("wifi off") || command.contains("turn off wifi") -> handleWifi(false)
            command.contains("bluetooth on") -> handleBluetooth(true)
            command.contains("bluetooth off") -> handleBluetooth(false)
            command.contains("brightness") -> handleBrightness(command)
            command.contains("airplane mode") -> handleAirplaneMode()

            // === BATTERY ===
            command.contains("battery") -> BatteryHelper.getBatteryStatus(context)

            // === NAVIGATION ===
            command.contains("navigate") || command.contains("directions") || command.contains("take me to") -> handleNavigation(command)

            // === CAMERA ===
            command.contains("take photo") || command.contains("take picture") || command.contains("selfie") -> handleCamera(command)

            // === WEATHER ===
            command.contains("weather") -> handleWeather()

            // === CALCULATOR ===
            command.contains("calculate") || command.contains("what is") && command.contains(Regex("\\d")) -> handleCalculate(command)

            // === PHONE SETTINGS ===
            command.contains("screenshot") -> "Taking screenshot for you."
            command.contains("lock") && command.contains("phone") -> "Locking your phone now."
            command.contains("restart") -> "Please restart your phone manually for safety."

            // === CONTACTS ===
            command.contains("contact") -> handleContacts(command)

            // === JOKES / FUN ===
            command.contains("joke") -> tellJoke()
            command.contains("how are you") -> "I'm doing great, Allen! Always ready to help you."
            command.contains("who are you") || command.contains("your name") -> "I am MAX, your personal voice assistant built by Allen."
            command.contains("what can you do") || command.contains("help") -> listCommands()

            // === STOP / EXIT ===
            command.contains("stop") || command.contains("bye") || command.contains("exit") -> "Goodbye, Allen! I'm always here when you need me."

            else -> "Sorry Allen, I didn't understand that command. Say 'what can you do' to hear all my features."
        }
    }

    private fun handleCall(command: String): String {
        val name = extractName(command, "call")
        val phone = ContactHelper.getPhoneNumber(context, name)
        return if (phone != null) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Calling $name now."
        } else if (name.isNotEmpty()) {
            "I couldn't find $name in your contacts."
        } else {
            "Who would you like to call?"
        }
    }

    private fun handleRedial(): String {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return "Redialing last number."
    }

    private fun handleMessage(command: String): String {
        return when {
            command.contains("whatsapp") -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "Opening WhatsApp for you."
            }
            else -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "Opening messages for you."
            }
        }
    }

    private fun toggleFlashlight(command: String): String {
        return try {
            val cameraId = cameraManager.cameraIdList[0]
            flashlightOn = command.contains("on") || (!flashlightOn && !command.contains("off"))
            cameraManager.setTorchMode(cameraId, flashlightOn)
            if (flashlightOn) "Flashlight turned on." else "Flashlight turned off."
        } catch (e: Exception) {
            "Couldn't toggle flashlight."
        }
    }

    private fun adjustVolume(increase: Boolean): String {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, direction, AudioManager.FLAG_SHOW_UI)
        return if (increase) "Volume increased." else "Volume decreased."
    }

    private fun mutePhone(): String {
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        return "Phone muted."
    }

    private fun unmutePhone(): String {
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        return "Phone unmuted."
    }

    private fun setSilentMode(): String {
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        return "Phone set to vibrate mode."
    }

    private fun handleOpenApp(command: String): String {
        val appMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "twitter" to "com.twitter.android",
            "telegram" to "org.telegram.messenger",
            "snapchat" to "com.snapchat.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "spotify" to "com.spotify.music",
            "camera" to "android.media.action.IMAGE_CAPTURE",
            "gallery" to "com.android.gallery3d",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "settings" to "com.android.settings",
            "calculator" to "com.android.calculator2",
            "calendar" to "com.google.android.calendar",
            "contacts" to "com.android.contacts",
            "clock" to "com.android.deskclock",
            "gmail" to "com.google.android.gm",
            "drive" to "com.google.android.apps.docs",
            "playstore" to "com.android.vending"
        )

        val appName = appMap.keys.firstOrNull { command.contains(it) }
        return if (appName != null) {
            val packageName = appMap[appName]!!
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    ?: Intent(packageName).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "Opening $appName."
            } catch (e: Exception) {
                "Couldn't find $appName on your phone."
            }
        } else {
            "Which app would you like to open?"
        }
    }

    private fun handleYouTubePlay(command: String): String {
        val query = command
            .replace("play", "")
            .replace("on youtube", "")
            .replace("youtube", "")
            .trim()
        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Searching YouTube for $query."
    }

    private fun handleMusicPlay(command: String): String {
        val query = command
            .replace("play", "")
            .replace("search song", "")
            .replace("music", "")
            .trim()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://open.spotify.com/search/$query")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return if (query.isNotEmpty()) "Playing $query." else "Opening music."
    }

    private fun handleWebSearch(command: String): String {
        val query = command
            .replace("search", "")
            .replace("google", "")
            .replace("for", "")
            .trim()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=$query")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Searching for $query."
    }

    private fun handleAlarm(command: String): String {
        // Extract hour from command like "set alarm for 7 AM"
        val timePattern = Regex("(\\d+)\\s*(am|pm|AM|PM)?")
        val match = timePattern.find(command)
        return if (match != null) {
            val hour = match.groupValues[1].toInt()
            val isPM = match.groupValues[2].lowercase() == "pm"
            val actualHour = if (isPM && hour != 12) hour + 12 else hour
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, actualHour)
                putExtra(AlarmClock.EXTRA_MINUTES, 0)
                putExtra(AlarmClock.EXTRA_MESSAGE, "MAX Alarm")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Alarm set for $hour ${if (isPM) "PM" else "AM"}."
        } else {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opening alarms."
        }
    }

    private fun handleTimer(command: String): String {
        val pattern = Regex("(\\d+)\\s*(minute|min|second|sec|hour|hr)")
        val match = pattern.find(command)
        return if (match != null) {
            val amount = match.groupValues[1].toInt()
            val unit = match.groupValues[2]
            val seconds = when {
                unit.startsWith("hour") || unit.startsWith("hr") -> amount * 3600
                unit.startsWith("min") -> amount * 60
                else -> amount
            }
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Timer set for $amount $unit."
        } else {
            "How long should the timer be?"
        }
    }

    private fun handleWifi(enable: Boolean): String {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return if (enable) "Opening WiFi settings to turn on." else "Opening WiFi settings to turn off."
    }

    private fun handleBluetooth(enable: Boolean): String {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return if (enable) "Opening Bluetooth settings." else "Opening Bluetooth settings to turn off."
    }

    private fun handleBrightness(command: String): String {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening display settings for brightness."
    }

    private fun handleAirplaneMode(): String {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening airplane mode settings."
    }

    private fun handleNavigation(command: String): String {
        val destination = command
            .replace("navigate to", "")
            .replace("directions to", "")
            .replace("take me to", "")
            .trim()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=$destination")
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Navigating to $destination."
    }

    private fun handleCamera(command: String): String {
        val isSelfie = command.contains("selfie") || command.contains("front")
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            if (isSelfie) putExtra("android.intent.extras.CAMERA_FACING", 1)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return if (isSelfie) "Opening front camera for a selfie." else "Opening camera."
    }

    private fun handleWeather(): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.weather.com")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening weather for you."
    }

    private fun handleCalculate(command: String): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.calculator2")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening calculator."
    }

    private fun handleContacts(command: String): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://contacts/people")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening contacts."
    }

    private fun extractName(command: String, action: String): String {
        return command.replace(action, "").trim()
    }

    private fun tellJoke(): String {
        val jokes = listOf(
            "Why don't scientists trust atoms? Because they make up everything!",
            "Why did the phone go to school? To improve its call-culus!",
            "Why do programmers prefer dark mode? Because light attracts bugs!",
            "What do you call a sleeping dinosaur? A dino-snore!",
            "Why can't you give Elsa a balloon? Because she'll let it go!"
        )
        return jokes.random()
    }

    private fun listCommands(): String {
        return "I can help you with: making calls, sending messages, WhatsApp, opening apps like YouTube and Instagram, " +
            "flashlight, volume control, setting alarms and timers, WiFi and Bluetooth, navigation, camera, weather, " +
            "web search, battery status, and much more. Just ask!"
    }
}
