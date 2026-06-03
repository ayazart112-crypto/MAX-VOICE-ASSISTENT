package com.allen.max.utils

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import java.text.SimpleDateFormat
import java.util.*

object DateTimeHelper {
    fun getTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "The time is ${sdf.format(Date())}."
    }

    fun getDate(): String {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return "Today is ${sdf.format(Date())}."
    }

    fun getDay(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return "Today is ${sdf.format(Date())}."
    }
}

object ContactHelper {
    fun getPhoneNumber(context: Context, name: String): String? {
        if (name.isBlank()) return null
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val contactName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                if (contactName.lowercase().contains(name.lowercase())) {
                    return it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                }
            }
        }
        return null
    }

    fun getLastDialedNumber(context: Context): String? {
        val uri = android.provider.CallLog.Calls.CONTENT_URI
        val projection = arrayOf(android.provider.CallLog.Calls.NUMBER)
        val selection = "${android.provider.CallLog.Calls.TYPE} = ?"
        val selectionArgs = arrayOf(android.provider.CallLog.Calls.OUTGOING_TYPE.toString())
        val sortOrder = "${android.provider.CallLog.Calls.DATE} DESC"
        
        val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER))
            }
        }
        return null
    }
}

object BatteryHelper {
    fun getBatteryStatus(context: Context): String {
        val batteryIntent = context.registerReceiver(null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val plugged = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val percentage = if (level != -1 && scale != -1) (level * 100 / scale) else -1
        val charging = plugged != 0
        return if (percentage != -1) {
            "Battery is at $percentage percent. ${if (charging) "Charging." else "Not charging."}"
        } else {
            "Couldn't get battery status."
        }
    }
}
