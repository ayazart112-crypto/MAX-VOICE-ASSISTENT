package com.allen.max.commands

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import com.allen.max.utils.ContactHelper
import com.allen.max.utils.DateTimeHelper
import com.allen.max.utils.BatteryHelper
import com.allen.max.utils.AIHelper
import com.allen.max.MainActivity

class CommandProcessor(private val context: Context, private val apiKey: String) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var flashlightOn = false

    fun process(command: String): String {
        val cmd = command.lowercase()
        return when {
            cmd.contains("time") || cmd.contains("waqt") -> DateTimeHelper.getTime()
            cmd.contains("date") || cmd.contains("tarikh") || cmd.contains("today") -> DateTimeHelper.getDate()
            cmd.contains("day") || cmd.contains("din") -> DateTimeHelper.getDay()
            (cmd.contains("call") || cmd.contains("phone")) && !cmd.contains("recall") -> handleCall(cmd)
            cmd.contains("redial") || cmd.contains("call back") -> handleRedial()
            (cmd.contains("whatsapp") || cmd.contains("watsapp")) && (cmd.contains("send") || cmd.contains("message") || cmd.contains("text") || cmd.contains("bhejo")) -> handleWhatsAppMessage(cmd)
            (cmd.contains("send") || cmd.contains("bhejo")) && cmd.contains("sms") -> handleSms(cmd)
            cmd.contains("flashlight") || cmd.contains("torch") || cmd.contains("light") -> toggleFlashlight(cmd)
            cmd.contains("volume up") || cmd.contains("increase volume") || cmd.contains("awaz tez") -> adjustVolume(true)
            cmd.contains("volume down") || cmd.contains("decrease volume") || cmd.contains("awaz kam") -> adjustVolume(false)
            cmd.contains("mute") || cmd.contains("khamosh") -> mutePhone()
            cmd.contains("unmute") || cmd.contains("silent off") -> unmutePhone()
            cmd.contains("silent") -> setSilentMode()
            cmd.contains("play") || cmd.contains("chalao") || cmd.contains("sunao") -> handleYouTubePlay(cmd)
            cmd.contains("open") || cmd.contains("launch") || cmd.contains("kholo") -> handleOpenApp(cmd)
            cmd.contains("search") || cmd.contains("google") || cmd.contains("pucho") -> handleWebSearch(cmd)
            cmd.contains("alarm") || cmd.contains("wake me") -> handleAlarm(cmd)
            cmd.contains("timer") -> handleTimer(cmd)
            cmd.contains("wifi") -> handleWifi(cmd.contains("on") || cmd.contains("kholo"))
            cmd.contains("bluetooth") -> handleBluetooth(cmd.contains("on"))
            cmd.contains("brightness") || cmd.contains("roshni") -> handleBrightness(cmd)
            cmd.contains("airplane mode") -> handleAirplaneMode()
            cmd.contains("battery") -> BatteryHelper.getBatteryStatus(context)
            cmd.contains("news") || cmd.contains("khabar") -> { askAI("Tell me the latest global news briefly."); "PROCESS_ASYNC" }
            cmd.contains("fact") || cmd.contains("haqeeqat") -> { askAI("Tell me an interesting random fact."); "PROCESS_ASYNC" }
            cmd.contains("navigate") || cmd.contains("directions") || cmd.contains("rasta") -> handleNavigation(cmd)
            cmd.contains("photo") || cmd.contains("picture") || cmd.contains("tasveer") || cmd.contains("camera") -> handleCamera(cmd)
            cmd.contains("weather") || cmd.contains("mausam") -> handleWeather()
            cmd.contains("contact") -> handleContacts(cmd)
            cmd.contains("joke") || cmd.contains("latifa") -> tellJoke()
            cmd.contains("how are you") || cmd.contains("kaise ho") -> "I'm performing at peak capacity, Allen! Main bilkul theek hoon. How can I help?"
            cmd.contains("who are you") || cmd.contains("your name") || cmd.contains("naam") -> "I am MAX, your personal AI voice assistant. I was created to make your life easier."
            cmd.contains("what can you do") || cmd.contains("help") || cmd.contains("madad") -> listCommands()
            cmd.contains("stop") || cmd.contains("bye") || cmd.contains("exit") -> "Shutting down. Goodbye, Allen!"
            else -> {
                askAI(cmd)
                "PROCESS_ASYNC"
            }
        }
    }

    private fun askAI(command: String) {
        (context as? MainActivity)?.updateResponse("Checking memory banks...")
        AIHelper.askAI(command, apiKey) { response ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                (context as? MainActivity)?.updateResponse(response)
                (context as? MainActivity)?.speak(response)
            }
        }
    }

    private fun handleWhatsAppMessage(command: String): String {
        val toIndex = command.indexOf(" to ")
        val sayIndex = command.indexOf(" say ")
        val name = when {
            toIndex != -1 && sayIndex != -1 -> command.substring(toIndex + 4, sayIndex).trim()
            toIndex != -1 -> command.substring(toIndex + 4).trim().split(" ").firstOrNull() ?: ""
            else -> ""
        }
        val message = if (sayIndex != -1) command.substring(sayIndex + 5).trim() else ""
        val phone = if (name.isNotEmpty()) ContactHelper.getPhoneNumber(context, name) else null
        return if (phone != null && message.isNotEmpty()) {
            val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            "Sending WhatsApp message to $name."
        } else if (phone != null) {
            val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            "Opening WhatsApp chat with $name."
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            if (name.isNotEmpty()) "Couldn't find $name. Opening WhatsApp." else "Opening WhatsApp."
        }
    }

    private fun handleYouTubePlay(command: String): String {
        val query = command.replace("play", "").replace("on youtube", "")
            .replace("youtube", "").replace("song", "").replace("music", "").trim()
        return if (query.isNotEmpty()) {
            try {
                context.startActivity(Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                "Searching YouTube for $query."
            } catch (e: Exception) {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                "Searching YouTube for $query."
            }
        } else {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            "Opening YouTube."
        }
    }

    private fun handleSms(command: String): String {
        val name = command.replace("send sms to", "").replace("send sms", "").trim().split(" ").firstOrNull() ?: ""
        val phone = ContactHelper.getPhoneNumber(context, name)
        val message = if (command.contains(" say ")) command.substringAfter(" say ").trim() else ""
        context.startActivity(Intent(Intent.ACTION_VIEW, if (phone != null) Uri.parse("sms:$phone") else Uri.parse("sms:")).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return if (phone != null) "Opening SMS to $name." else "Opening messages."
    }

    private fun handleCall(command: String): String {
        val name = command.replace("call", "").trim()
        val phone = ContactHelper.getPhoneNumber(context, name)
        return if (phone != null) {
            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            "Calling $name now."
        } else if (name.isNotEmpty()) {
            "I couldn't find $name in your contacts."
        } else { "Who would you like to call?" }
    }

    private fun handleRedial(): String = "Redialing last number."

    private fun toggleFlashlight(command: String): String {
        return try {
            val cameraId = cameraManager.cameraIdList[0]
            flashlightOn = command.contains("on") || (!flashlightOn && !command.contains("off"))
            cameraManager.setTorchMode(cameraId, flashlightOn)
            if (flashlightOn) "Flashlight turned on." else "Flashlight turned off."
        } catch (e: Exception) { "Couldn't toggle flashlight." }
    }

    private fun adjustVolume(increase: Boolean): String {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_RING,
            if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        return if (increase) "Volume increased." else "Volume decreased."
    }

    private fun mutePhone(): String { audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT; return "Phone muted." }
    private fun unmutePhone(): String { audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL; return "Phone unmuted." }
    private fun setSilentMode(): String { audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE; return "Phone set to vibrate." }

    private fun handleOpenApp(command: String): String {
        val appMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "telegram" to "org.telegram.messenger",
            "tiktok" to "com.zhiliaoapp.musically",
            "spotify" to "com.spotify.music",
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
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(appMap[appName]!!)
                    ?: Intent(appMap[appName]!!).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "Opening $appName."
            } catch (e: Exception) { "Couldn't find $appName on your phone." }
        } else { "Which app would you like to open?" }
    }

    private fun handleWebSearch(command: String): String {
        val query = command.replace("search", "").replace("google", "").replace("for", "").trim()
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return "Searching for $query."
    }

    private fun handleAlarm(command: String): String {
        val match = Regex("(\\d+)\\s*(am|pm|AM|PM)?").find(command)
        return if (match != null) {
            val hour = match.groupValues[1].toInt()
            val isPM = match.groupValues[2].lowercase() == "pm"
            val actualHour = if (isPM && hour != 12) hour + 12 else hour
            context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, actualHour)
                putExtra(AlarmClock.EXTRA_MINUTES, 0)
                putExtra(AlarmClock.EXTRA_MESSAGE, "MAX Alarm")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            "Alarm set for $hour ${if (isPM) "PM" else "AM"}."
        } else {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            "Opening alarms."
        }
    }

    private fun handleTimer(command: String): String {
        val match = Regex("(\\d+)\\s*(minute|min|second|sec|hour|hr)").find(command)
        return if (match != null) {
            val amount = match.groupValues[1].toInt()
            val unit = match.groupValues[2]
            val seconds = when {
                unit.startsWith("hour") || unit.startsWith("hr") -> amount * 3600
                unit.startsWith("min") -> amount * 60
                else -> amount
            }
            context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            "Timer set for $amount $unit."
        } else { "How long should the timer be?" }
    }

    private fun handleWifi(enable: Boolean): String {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        return if (enable) "Opening WiFi settings." else "Opening WiFi settings to turn off."
    }

    private fun handleBluetooth(enable: Boolean): String {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        return "Opening Bluetooth settings."
    }

    private fun handleBrightness(command: String): String {
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        return "Opening display settings."
    }

    private fun handleAirplaneMode(): String {
        context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        return "Opening airplane mode settings."
    }

    private fun handleNavigation(command: String): String {
        val dest = command.replace("navigate to", "").replace("directions to", "").replace("take me to", "").trim()
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=${Uri.encode(dest)}")
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return "Navigating to $dest."
    }

    private fun handleCamera(command: String): String {
        val isSelfie = command.contains("selfie") || command.contains("front")
        context.startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            if (isSelfie) putExtra("android.intent.extras.CAMERA_FACING", 1)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return if (isSelfie) "Opening front camera." else "Opening camera."
    }

    private fun handleWeather(): String {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.weather.com")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return "Opening weather."
    }

    private fun handleContacts(command: String): String {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://contacts/people")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return "Opening contacts."
    }

    private fun tellJoke(): String {
        return listOf(
            "Why don't scientists trust atoms? Because they make up everything!",
            "Why do programmers prefer dark mode? Because light attracts bugs!",
            "What do you call a sleeping dinosaur? A dino-snore!"
        ).random()
    }

    private fun listCommands(): String {
        return "I can: make calls, send WhatsApp messages, play songs on YouTube, open apps, " +
            "control flashlight, volume, alarms, timers, WiFi, Bluetooth, navigate, take photos, " +
            "check weather, search the web, check battery, and answer any question with AI!"
    }
}
