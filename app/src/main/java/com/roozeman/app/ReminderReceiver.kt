package com.roozeman.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        if (taskId == -1L) return
        val db = DbHelper(context).readableDatabase
        val cursor = db.rawQuery("SELECT title, done FROM tasks WHERE id=?", arrayOf(taskId.toString()))
        if (cursor.moveToFirst()) {
            val title = cursor.getString(0)
            val done = cursor.getInt(1) == 1
            cursor.close()
            if (!done) {
                createNotificationChannel(context)
                showTaskNotification(context, taskId, title)
            }
        } else {
            cursor.close()
        }
    }
}

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.roozeman.app.ACTION_TASK_DONE") {
            val taskId = intent.getLongExtra("taskId", -1)
            if (taskId == -1L) return
            val db = DbHelper(context).writableDatabase
            updateTaskDone(db, taskId, true)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(taskId.toInt())
        }
    }
}
