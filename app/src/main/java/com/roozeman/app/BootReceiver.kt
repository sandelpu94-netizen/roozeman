package com.roozeman.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = DbHelper(context).readableDatabase
            val cursor = db.rawQuery(
                "SELECT id, reminderHour, reminderMinute FROM tasks WHERE done=0 AND reminderHour IS NOT NULL",
                null
            )
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val hour = cursor.getInt(1)
                val minute = cursor.getInt(2)
                scheduleTaskReminder(context, id, hour, minute)
            }
            cursor.close()
        }
    }
}
