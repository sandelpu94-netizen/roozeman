#!/bin/bash
# اسکریپت همگام‌سازی کامل روزمن — همه فایل‌هایی که تا الان ساختیم رو تمیز و یکجا می‌نویسه
# از ریشه‌ی ریپو اجرا کن: bash roozeman_full_sync.sh
set -e

echo "۱) بازنویسی کامل AndroidManifest.xml"
cat > app/src/main/AndroidManifest.xml << 'EOF'
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity
            android:name="MainActivity"
            android:exported="true">
            <intent-filter>
                <action
                    android:name="android.intent.action.MAIN" />

                <category
                    android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <receiver android:name=".ReminderReceiver" android:exported="false" />
        <receiver android:name=".TaskActionReceiver" android:exported="false" />
        <receiver android:name=".BootReceiver" android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
        <receiver android:name=".TaskWidgetProvider" android:exported="false">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/task_widget_info" />
        </receiver>
    </application>
</manifest>
EOF

echo "۲) بازنویسی DbHelper.kt (v6)"
cat > app/src/main/java/com/roozeman/app/DbHelper.kt << 'EOF'
package com.roozeman.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TaskItem(
    val id: Long,
    val title: String,
    val done: Boolean,
    val recurrence: String,
    val reminderHour: Int?,
    val reminderMinute: Int?
)
data class IdeaItem(val id: Long, val title: String, val date: String, val tag: String)
data class GoalItem(val id: Long, val title: String, val progress: Float, val daysLeft: Int, val recurrence: String, val targetDate: String?)
data class HabitItem(val id: Long, val title: String, val days: List<Boolean>, val streak: Int)
data class NoteItem(val id: Long, val text: String)
data class ScheduleItem(val id: Long, val time: String, val label: String, val done: Boolean)

class DbHelper(context: Context) : SQLiteOpenHelper(context, "roozeman.db", null, 6) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, done INTEGER, recurrence TEXT DEFAULT 'none', reminderHour INTEGER, reminderMinute INTEGER)")
        db.execSQL("CREATE TABLE ideas (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, date TEXT, tag TEXT)")
        db.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, progress REAL, daysLeft INTEGER, recurrence TEXT DEFAULT 'none', targetDate TEXT)")
        db.execSQL("CREATE TABLE habits (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, d0 INTEGER, d1 INTEGER, d2 INTEGER, d3 INTEGER, d4 INTEGER, d5 INTEGER, d6 INTEGER, streak INTEGER)")
        db.execSQL("CREATE TABLE habit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, habit_id INTEGER, date TEXT, done INTEGER)")
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT)")
        db.execSQL("CREATE TABLE schedule (id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT, label TEXT, done INTEGER DEFAULT 0)")

        db.execSQL("INSERT INTO tasks (title, done, recurrence) VALUES ('انجام کاری که امروز مهم‌تر از همه است', 0, 'none')")
        db.execSQL("INSERT INTO tasks (title, done, recurrence) VALUES ('چک کردن ایمیل‌ها', 1, 'none')")
        db.execSQL("INSERT INTO tasks (title, done, recurrence) VALUES ('۳۰ دقیقه پیاده‌روی', 1, 'none')")
        db.execSQL("INSERT INTO tasks (title, done, recurrence) VALUES ('مطالعه ۲۰ دقیقه', 0, 'none')")

        db.execSQL("INSERT INTO goals (title, progress, daysLeft, recurrence) VALUES ('یادگیری زبان انگلیسی', 0.6, 12, 'none')")
        db.execSQL("INSERT INTO goals (title, progress, daysLeft, recurrence) VALUES ('ورزش منظم', 0.3, 45, 'monthly')")
        db.execSQL("INSERT INTO goals (title, progress, daysLeft, recurrence) VALUES ('مطالعه ۱۲ کتاب امسال', 0.4, 90, 'none')")

        db.execSQL("INSERT INTO ideas (title, date, tag) VALUES ('طراحی یک محصول جدید', '۱۳ شهریور', '⭐ مهم')")
        db.execSQL("INSERT INTO ideas (title, date, tag) VALUES ('پیشنهاد ویژگی جدید برای اپ', '۱۰ شهریور', '💡 ایده')")

        db.execSQL("INSERT INTO habits (title, d0, d1, d2, d3, d4, d5, d6, streak) VALUES ('ورزش', 0, 0, 0, 0, 0, 0, 0, 0)")

        db.execSQL("INSERT INTO schedule (time, label) VALUES ('۰۸:۰۰', 'شروع روز')")
        db.execSQL("INSERT INTO schedule (time, label) VALUES ('۱۰:۰۰', 'کار اصلی')")
        db.execSQL("INSERT INTO schedule (time, label) VALUES ('۱۳:۰۰', 'ناهار و استراحت')")
        db.execSQL("INSERT INTO schedule (time, label) VALUES ('۱۶:۰۰', 'مطالعه')")
        db.execSQL("INSERT INTO schedule (time, label) VALUES ('۲۰:۰۰', 'وقت آزاد')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, d0 INTEGER, d1 INTEGER, d2 INTEGER, d3 INTEGER, d4 INTEGER, d5 INTEGER, d6 INTEGER, streak INTEGER)")
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT)")
        }
        if (oldVersion < 4) {
            safeAlter(db, "ALTER TABLE tasks ADD COLUMN recurrence TEXT DEFAULT 'none'")
            safeAlter(db, "ALTER TABLE tasks ADD COLUMN reminderHour INTEGER")
            safeAlter(db, "ALTER TABLE tasks ADD COLUMN reminderMinute INTEGER")
            safeAlter(db, "ALTER TABLE goals ADD COLUMN recurrence TEXT DEFAULT 'none'")
            db.execSQL("CREATE TABLE IF NOT EXISTS habit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, habit_id INTEGER, date TEXT, done INTEGER)")
        }
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS schedule (id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT, label TEXT)")
            val cursor = db.rawQuery("SELECT COUNT(*) FROM schedule", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            if (count == 0) {
                db.execSQL("INSERT INTO schedule (time, label) VALUES ('۰۸:۰۰', 'شروع روز')")
                db.execSQL("INSERT INTO schedule (time, label) VALUES ('۱۰:۰۰', 'کار اصلی')")
                db.execSQL("INSERT INTO schedule (time, label) VALUES ('۱۳:۰۰', 'ناهار و استراحت')")
                db.execSQL("INSERT INTO schedule (time, label) VALUES ('۱۶:۰۰', 'مطالعه')")
                db.execSQL("INSERT INTO schedule (time, label) VALUES ('۲۰:۰۰', 'وقت آزاد')")
            }
        }
        if (oldVersion < 6) {
            safeAlter(db, "ALTER TABLE schedule ADD COLUMN done INTEGER DEFAULT 0")
            safeAlter(db, "ALTER TABLE goals ADD COLUMN targetDate TEXT")
        }
    }

    private fun safeAlter(db: SQLiteDatabase, sql: String) {
        try {
            db.execSQL(sql)
        } catch (e: SQLiteException) {
        }
    }
}

fun loadTasks(db: SQLiteDatabase): List<TaskItem> {
    val list = mutableListOf<TaskItem>()
    val cursor = db.rawQuery("SELECT id, title, done, recurrence, reminderHour, reminderMinute FROM tasks ORDER BY id ASC", null)
    while (cursor.moveToNext()) {
        list.add(
            TaskItem(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getInt(2) == 1,
                cursor.getString(3) ?: "none",
                if (cursor.isNull(4)) null else cursor.getInt(4),
                if (cursor.isNull(5)) null else cursor.getInt(5)
            )
        )
    }
    cursor.close()
    return list
}

fun insertTask(db: SQLiteDatabase, title: String, recurrence: String = "none", reminderHour: Int? = null, reminderMinute: Int? = null): Long {
    val values = ContentValues()
    values.put("title", title)
    values.put("done", 0)
    values.put("recurrence", recurrence)
    if (reminderHour != null) values.put("reminderHour", reminderHour) else values.putNull("reminderHour")
    if (reminderMinute != null) values.put("reminderMinute", reminderMinute) else values.putNull("reminderMinute")
    return db.insert("tasks", null, values)
}

fun updateTaskDone(db: SQLiteDatabase, id: Long, done: Boolean): Long? {
    val values = ContentValues()
    values.put("done", if (done) 1 else 0)
    db.update("tasks", values, "id = ?", arrayOf(id.toString()))

    if (!done) return null

    val cursor = db.rawQuery("SELECT title, recurrence, reminderHour, reminderMinute FROM tasks WHERE id=?", arrayOf(id.toString()))
    var newId: Long? = null
    if (cursor.moveToFirst()) {
        val recurrence = cursor.getString(1) ?: "none"
        if (recurrence == "monthly") {
            val title = cursor.getString(0)
            val rh = if (cursor.isNull(2)) null else cursor.getInt(2)
            val rm = if (cursor.isNull(3)) null else cursor.getInt(3)
            cursor.close()
            newId = insertTask(db, title, recurrence, rh, rm)
        } else {
            cursor.close()
        }
    } else {
        cursor.close()
    }
    return newId
}

fun deleteTask(db: SQLiteDatabase, id: Long) {
    db.delete("tasks", "id = ?", arrayOf(id.toString()))
}

fun loadIdeas(db: SQLiteDatabase): List<IdeaItem> {
    val list = mutableListOf<IdeaItem>()
    val cursor = db.rawQuery("SELECT id, title, date, tag FROM ideas ORDER BY id DESC", null)
    while (cursor.moveToNext()) {
        list.add(IdeaItem(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)))
    }
    cursor.close()
    return list
}

fun insertIdea(db: SQLiteDatabase, title: String) {
    val values = ContentValues()
    values.put("title", title)
    values.put("date", "امروز")
    values.put("tag", "💡 ایده")
    db.insert("ideas", null, values)
}

fun deleteIdea(db: SQLiteDatabase, id: Long) {
    db.delete("ideas", "id = ?", arrayOf(id.toString()))
}

fun loadGoals(db: SQLiteDatabase): List<GoalItem> {
    val list = mutableListOf<GoalItem>()
    val cursor = db.rawQuery("SELECT id, title, progress, daysLeft, recurrence, targetDate FROM goals ORDER BY id ASC", null)
    while (cursor.moveToNext()) {
        val targetDate = if (cursor.isNull(5)) null else cursor.getString(5)
        val daysLeft = if (targetDate != null) {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(targetDate)).toInt()
        } else {
            cursor.getInt(3)
        }
        list.add(
            GoalItem(
                cursor.getLong(0), cursor.getString(1), cursor.getFloat(2),
                daysLeft, cursor.getString(4) ?: "none", targetDate
            )
        )
    }
    cursor.close()
    return list
}

fun insertGoal(db: SQLiteDatabase, title: String, recurrence: String = "none") {
    val values = ContentValues()
    values.put("title", title)
    values.put("progress", 0f)
    values.put("daysLeft", 0)
    values.put("recurrence", recurrence)
    db.insert("goals", null, values)
}

fun deleteGoal(db: SQLiteDatabase, id: Long) {
    db.delete("goals", "id = ?", arrayOf(id.toString()))
}

fun updateGoalProgress(db: SQLiteDatabase, id: Long, progress: Float) {
    val clamped = progress.coerceIn(0f, 1f)
    if (clamped >= 1f) {
        val cursor = db.rawQuery("SELECT recurrence, targetDate FROM goals WHERE id=?", arrayOf(id.toString()))
        if (cursor.moveToFirst()) {
            val recurrence = cursor.getString(0) ?: "none"
            val targetDate = if (cursor.isNull(1)) null else cursor.getString(1)
            cursor.close()
            if (recurrence == "monthly") {
                val values = ContentValues()
                values.put("progress", 0f)
                if (targetDate != null) {
                    values.put("targetDate", LocalDate.parse(targetDate).plusMonths(1).toString())
                }
                db.update("goals", values, "id = ?", arrayOf(id.toString()))
                return
            }
        } else {
            cursor.close()
        }
    }
    val values = ContentValues()
    values.put("progress", clamped)
    db.update("goals", values, "id = ?", arrayOf(id.toString()))
}

fun setGoalTargetDate(db: SQLiteDatabase, id: Long, dateIso: String) {
    val values = ContentValues()
    values.put("targetDate", dateIso)
    db.update("goals", values, "id = ?", arrayOf(id.toString()))
}

private fun todayPersianWeekIndex(): Int {
    return (LocalDate.now().dayOfWeek.value + 1) % 7
}

fun loadHabits(db: SQLiteDatabase): List<HabitItem> {
    val list = mutableListOf<HabitItem>()
    val cursor = db.rawQuery("SELECT id, title FROM habits ORDER BY id ASC", null)
    val idsTitles = mutableListOf<Pair<Long, String>>()
    while (cursor.moveToNext()) idsTitles.add(cursor.getLong(0) to cursor.getString(1))
    cursor.close()

    val today = LocalDate.now()
    val todayIdx = todayPersianWeekIndex()

    for ((id, title) in idsTitles) {
        val days = (0..6).map { i ->
            val date = today.minusDays((todayIdx - i).toLong())
            isHabitDoneOnDate(db, id, date.toString())
        }
        val streak = computeHabitStreak(db, id)
        list.add(HabitItem(id, title, days, streak))
    }
    return list
}

fun deleteHabit(db: SQLiteDatabase, id: Long) {
    db.delete("habit_logs", "habit_id = ?", arrayOf(id.toString()))
    db.delete("habits", "id = ?", arrayOf(id.toString()))
}

fun insertHabit(db: SQLiteDatabase, title: String): Long {
    val values = ContentValues()
    values.put("title", title)
    values.put("d0", 0); values.put("d1", 0); values.put("d2", 0); values.put("d3", 0)
    values.put("d4", 0); values.put("d5", 0); values.put("d6", 0)
    values.put("streak", 0)
    return db.insert("habits", null, values)
}

fun isHabitDoneOnDate(db: SQLiteDatabase, habitId: Long, date: String): Boolean {
    val cursor = db.rawQuery("SELECT done FROM habit_logs WHERE habit_id=? AND date=?", arrayOf(habitId.toString(), date))
    val result = if (cursor.moveToFirst()) cursor.getInt(0) == 1 else false
    cursor.close()
    return result
}

fun updateHabitDay(db: SQLiteDatabase, id: Long, dayIndex: Int, value: Boolean) {
    val today = LocalDate.now()
    val todayIdx = todayPersianWeekIndex()
    val date = today.minusDays((todayIdx - dayIndex).toLong()).toString()

    val cursor = db.rawQuery("SELECT id FROM habit_logs WHERE habit_id=? AND date=?", arrayOf(id.toString(), date))
    if (cursor.moveToFirst()) {
        val logId = cursor.getLong(0)
        cursor.close()
        db.execSQL("UPDATE habit_logs SET done=? WHERE id=?", arrayOf(if (value) 1 else 0, logId))
    } else {
        cursor.close()
        val values = ContentValues()
        values.put("habit_id", id)
        values.put("date", date)
        values.put("done", if (value) 1 else 0)
        db.insert("habit_logs", null, values)
    }
}

fun computeHabitStreak(db: SQLiteDatabase, habitId: Long): Int {
    val cursor = db.rawQuery("SELECT date, done FROM habit_logs WHERE habit_id=?", arrayOf(habitId.toString()))
    val map = HashMap<String, Boolean>()
    while (cursor.moveToNext()) {
        map[cursor.getString(0)] = cursor.getInt(1) == 1
    }
    cursor.close()

    var streak = 0
    var d = LocalDate.now()
    if (map[d.toString()] != true) {
        d = d.minusDays(1)
    }
    while (map[d.toString()] == true) {
        streak++
        d = d.minusDays(1)
    }
    return streak
}

fun loadNotes(db: SQLiteDatabase): List<NoteItem> {
    val list = mutableListOf<NoteItem>()
    val cursor = db.rawQuery("SELECT id, text FROM notes ORDER BY id DESC", null)
    while (cursor.moveToNext()) {
        list.add(NoteItem(cursor.getLong(0), cursor.getString(1)))
    }
    cursor.close()
    return list
}

fun insertNote(db: SQLiteDatabase, text: String) {
    val values = ContentValues()
    values.put("text", text)
    db.insert("notes", null, values)
}

fun deleteNote(db: SQLiteDatabase, id: Long) {
    db.delete("notes", "id = ?", arrayOf(id.toString()))
}

fun loadSchedule(db: SQLiteDatabase): List<ScheduleItem> {
    val list = mutableListOf<ScheduleItem>()
    val cursor = db.rawQuery("SELECT id, time, label, done FROM schedule ORDER BY time ASC", null)
    while (cursor.moveToNext()) {
        list.add(ScheduleItem(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3) == 1))
    }
    cursor.close()
    return list
}

fun insertSchedule(db: SQLiteDatabase, time: String, label: String): Long {
    val values = ContentValues()
    values.put("time", time)
    values.put("label", label)
    values.put("done", 0)
    return db.insert("schedule", null, values)
}

fun updateScheduleDone(db: SQLiteDatabase, id: Long, done: Boolean) {
    val values = ContentValues()
    values.put("done", if (done) 1 else 0)
    db.update("schedule", values, "id = ?", arrayOf(id.toString()))
}

fun deleteSchedule(db: SQLiteDatabase, id: Long) {
    db.delete("schedule", "id = ?", arrayOf(id.toString()))
}

fun resetAllData(db: SQLiteDatabase) {
    db.execSQL("DELETE FROM tasks")
    db.execSQL("DELETE FROM ideas")
    db.execSQL("DELETE FROM goals")
    db.execSQL("DELETE FROM notes")
    db.execSQL("DELETE FROM habit_logs")
    db.execSQL("UPDATE habits SET d0=0, d1=0, d2=0, d3=0, d4=0, d5=0, d6=0, streak=0")
    db.execSQL("UPDATE schedule SET done=0")
}
EOF

echo "۳) بازنویسی NotificationHelper.kt"
cat > app/src/main/java/com/roozeman/app/NotificationHelper.kt << 'EOF'
package com.roozeman.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

const val CHANNEL_ID = "roozeman_reminders"

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID, "یادآوری‌های روزمن", NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "یادآوری کارها و برنامه‌های روزانه"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

fun showTaskNotification(context: Context, taskId: Long, title: String) {
    val doneIntent = Intent(context, TaskActionReceiver::class.java).apply {
        action = "com.roozeman.app.ACTION_TASK_DONE"
        putExtra("taskId", taskId)
    }
    val donePendingIntent = PendingIntent.getBroadcast(
        context, taskId.toInt(), doneIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("یادآوری کار")
        .setContentText(title)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .addAction(android.R.drawable.checkbox_on_background, "انجام شد", donePendingIntent)
        .setAutoCancel(true)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(taskId.toInt(), notification)
}

fun scheduleTaskReminder(context: Context, taskId: Long, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("taskId", taskId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, taskId.toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
        )
    } catch (e: SecurityException) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}

fun cancelTaskReminder(context: Context, taskId: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, taskId.toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}
EOF

echo "۴) بازنویسی ReminderReceiver.kt"
cat > app/src/main/java/com/roozeman/app/ReminderReceiver.kt << 'EOF'
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
            TaskWidgetProvider.updateAll(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(taskId.toInt())
        }
    }
}
EOF

echo "۵) بازنویسی BootReceiver.kt"
cat > app/src/main/java/com/roozeman/app/BootReceiver.kt << 'EOF'
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
EOF

echo "۶) بازنویسی TaskWidgetProvider.kt"
cat > app/src/main/java/com/roozeman/app/TaskWidgetProvider.kt << 'EOF'
package com.roozeman.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class TaskWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_TOGGLE = "com.roozeman.app.ACTION_WIDGET_TOGGLE_TASK"
        const val EXTRA_TASK_ID = "taskId"
        val rowIds = intArrayOf(R.id.widget_task_1, R.id.widget_task_2, R.id.widget_task_3, R.id.widget_task_4, R.id.widget_task_5)

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                TaskWidgetProvider().onUpdate(context, manager, ids)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val db = DbHelper(context).readableDatabase
        val tasks = loadTasks(db).filter { !it.done }.take(5)

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.task_widget)

            for (i in rowIds.indices) {
                if (i < tasks.size) {
                    val task = tasks[i]
                    views.setTextViewText(rowIds[i], "☐ " + task.title)
                    views.setViewVisibility(rowIds[i], View.VISIBLE)

                    val toggleIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE
                        putExtra(EXTRA_TASK_ID, task.id)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, task.id.toInt(), toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(rowIds[i], pendingIntent)
                } else {
                    views.setViewVisibility(rowIds[i], View.GONE)
                }
            }

            views.setViewVisibility(R.id.widget_empty, if (tasks.isEmpty()) View.VISIBLE else View.GONE)

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
            if (taskId != -1L) {
                val db = DbHelper(context).writableDatabase
                updateTaskDone(db, taskId, true)
                updateAll(context)
            }
        }
    }
}
EOF

echo "۷) بازنویسی res/xml/task_widget_info.xml"
mkdir -p app/src/main/res/xml
cat > app/src/main/res/xml/task_widget_info.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="180dp"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/task_widget"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
EOF

echo "۸) بازنویسی res/layout/task_widget.xml"
mkdir -p app/src/main/res/layout
cat > app/src/main/res/layout/task_widget.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#FFFAF8FC"
    android:padding="12dp">

    <TextView
        android:id="@+id/widget_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="📋 کارهای امروز"
        android:textStyle="bold"
        android:textSize="16sp"
        android:textColor="#FF1D1B20"
        android:gravity="right" />

    <TextView
        android:id="@+id/widget_task_1"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:textSize="14sp"
        android:textColor="#FF1D1B20"
        android:gravity="right"
        android:visibility="gone" />

    <TextView
        android:id="@+id/widget_task_2"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="#FF1D1B20"
        android:gravity="right"
        android:visibility="gone" />

    <TextView
        android:id="@+id/widget_task_3"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="#FF1D1B20"
        android:gravity="right"
        android:visibility="gone" />

    <TextView
        android:id="@+id/widget_task_4"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="#FF1D1B20"
        android:gravity="right"
        android:visibility="gone" />

    <TextView
        android:id="@+id/widget_task_5"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:textColor="#FF1D1B20"
        android:gravity="right"
        android:visibility="gone" />

    <TextView
        android:id="@+id/widget_empty"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:text="کاری باقی نمانده 🎉"
        android:textSize="14sp"
        android:textColor="#FF1D1B20"
        android:gravity="right"
        android:visibility="gone" />
</LinearLayout>
EOF

echo "۹) بازنویسی app/build.gradle.kts (امضای ثابت)"
cat > app/build.gradle.kts << 'EOF'
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releasePropsFile = rootProject.file("release.properties")
val releaseProps = Properties()
if (releasePropsFile.exists()) {
    releaseProps.load(FileInputStream(releasePropsFile))
}

android {
    namespace = "com.roozeman.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.roozeman.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (releasePropsFile.exists()) {
                storeFile = rootProject.file(releaseProps.getProperty("storeFile"))
                storePassword = releaseProps.getProperty("storePassword")
                keyAlias = releaseProps.getProperty("keyAlias")
                keyPassword = releaseProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
}
EOF

echo "۱۰) بازنویسی کامل MainActivity.kt (همه قابلیت‌ها با هم: تیک برنامه، اهداف واقعی، ویجت)"
cat > app/src/main/java/com/roozeman/app/MainActivity.kt << 'MAINEOF'
package com.roozeman.app

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import java.time.LocalDate
import java.util.Calendar

fun toJalali(gyIn: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var jy = if (gyIn <= 1600) 0 else 979
    val gy2 = if (gyIn <= 1600) gyIn - 621 else gyIn - 1600
    val gy3 = if (gm > 2) gy2 + 1 else gy2
    var days = (365 * gy2) + ((gy3 + 3) / 4) - ((gy3 + 99) / 100) + ((gy3 + 399) / 400) - 80 + gd + gDaysInMonth[gm - 1]
    jy += 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
    val jd = 1 + (if (days < 186) (days % 31) else ((days - 186) % 30))
    return Triple(jy, jm, jd)
}

val persianMonths = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)
val persianWeekdays = listOf(
    "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
)
val persianWeekdaysShort = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

private val RoozemanLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03A9A4),
    background = Color(0xFFFAF8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1ECF6)
)
private val RoozemanDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    secondary = Color(0xFF4DD0CB),
    background = Color(0xFF141218),
    surface = Color(0xFF1D1B20)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoozemanApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoozemanApp() {
    var darkModeSetting by remember { mutableStateOf(0) }
    val isDark = when (darkModeSetting) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) RoozemanDarkColors else RoozemanLightColors
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val context = LocalContext.current
            val dbHelper = remember { DbHelper(context) }
            val db = remember { dbHelper.writableDatabase }

            val notifPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
            LaunchedEffect(Unit) {
                createNotificationChannel(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            var tasksVersion by remember { mutableStateOf(0) }
            var ideasVersion by remember { mutableStateOf(0) }
            var goalsVersion by remember { mutableStateOf(0) }
            var habitsVersion by remember { mutableStateOf(0) }
            var notesVersion by remember { mutableStateOf(0) }
            var scheduleVersion by remember { mutableStateOf(0) }

            var showAddSheet by remember { mutableStateOf(false) }
            var selectedTab by remember { mutableStateOf(0) }

            val onResetAll = {
                resetAllData(db)
                tasksVersion += 1
                ideasVersion += 1
                goalsVersion += 1
                habitsVersion += 1
                notesVersion += 1
                scheduleVersion += 1
                TaskWidgetProvider.updateAll(context)
            }

            Scaffold(
                bottomBar = {
                    RoozemanBottomBar(selected = selectedTab, onSelect = { selectedTab = it })
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن")
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (selectedTab) {
                        0 -> HomeScreen(db, tasksVersion, scheduleVersion, onTasksChanged = { tasksVersion += 1; TaskWidgetProvider.updateAll(context) }, onScheduleChanged = { scheduleVersion += 1 })
                        1 -> CalendarScreen()
                        2 -> GoalsScreen(db, goalsVersion, onGoalsChanged = { goalsVersion += 1 })
                        3 -> IdeasScreen(db, ideasVersion, onIdeasChanged = { ideasVersion += 1 })
                        else -> MoreScreen(
                            db = db,
                            habitsVersion = habitsVersion,
                            onHabitsChanged = { habitsVersion += 1 },
                            notesVersion = notesVersion,
                            onNotesChanged = { notesVersion += 1 },
                            darkModeSetting = darkModeSetting,
                            onDarkModeChange = { darkModeSetting = it },
                            onResetAll = onResetAll
                        )
                    }
                }

                if (showAddSheet) {
                    AddSheet(
                        onDismiss = { showAddSheet = false },
                        onAddTask = { title, recurrence, rh, rm ->
                            val newId = insertTask(db, title, recurrence, rh, rm)
                            tasksVersion += 1
                            TaskWidgetProvider.updateAll(context)
                            if (rh != null && rm != null) scheduleTaskReminder(context, newId, rh, rm)
                        },
                        onAddIdea = { title -> insertIdea(db, title); ideasVersion += 1 },
                        onAddGoal = { title, recurrence -> insertGoal(db, title, recurrence); goalsVersion += 1 },
                        onAddNote = { text -> insertNote(db, text); notesVersion += 1 },
                        onAddSchedule = { time, label -> insertSchedule(db, time, label); scheduleVersion += 1 },
                        onAddHabit = { title -> insertHabit(db, title); habitsVersion += 1 }
                    )
                }
            }
        }
    }
}

@Composable
fun isSystemInDarkTheme(): Boolean {
    return androidx.compose.foundation.isSystemInDarkTheme()
}

@Composable
fun HomeScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, scheduleVersion: Int, onTasksChanged: () -> Unit, onScheduleChanged: () -> Unit) {
    val context = LocalContext.current
    val tasks = remember(version) { loadTasks(db) }

    val today = LocalDate.now()
    val (jy, jm, jd) = toJalali(today.year, today.monthValue, today.dayOfMonth)
    val weekdayIndex = today.dayOfWeek.value % 7
    val weekdayName = persianWeekdays[weekdayIndex]
    val monthName = persianMonths[jm - 1]

    val schedule = remember(scheduleVersion) { loadSchedule(db) }
    val doneCount = tasks.count { it.done }
    val progress = if (tasks.isNotEmpty()) doneCount.toFloat() / tasks.size else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "سلام مهدی 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "$weekdayName، $jd $monthName $jy",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⭐ مهم‌ترین کار امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = tasks.firstOrNull()?.title ?: "کاری ثبت نشده")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "🕐 برنامه امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (schedule.isEmpty()) {
                    Text(text = "برنامه‌ای ثبت نشده")
                }
                schedule.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { checked ->
                                updateScheduleDone(db, item.id, checked)
                                onScheduleChanged()
                            }
                        )
                        Text(
                            text = item.time,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = item.label,
                            modifier = Modifier.weight(1f),
                            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
                        )
                        TextButton(onClick = { deleteSchedule(db, item.id); onScheduleChanged() }) { Text("✕") }
                    }
                    if (index != schedule.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "📋 کارهای امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (tasks.isEmpty()) {
                    Text(text = "هنوز کاری ثبت نشده")
                }
                tasks.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = task.done,
                            onCheckedChange = { checked ->
                                val newId = updateTaskDone(db, task.id, checked)
                                if (checked && task.reminderHour != null) {
                                    cancelTaskReminder(context, task.id)
                                }
                                if (newId != null && task.reminderHour != null && task.reminderMinute != null) {
                                    scheduleTaskReminder(context, newId, task.reminderHour, task.reminderMinute)
                                }
                                onTasksChanged()
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = task.title)
                            if (task.recurrence == "monthly" || task.reminderHour != null) {
                                Row {
                                    if (task.recurrence == "monthly") {
                                        Text(text = "🔁 ماهانه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (task.reminderHour != null) {
                                        Text(
                                            text = "⏰ ${"%02d".format(task.reminderHour)}:${"%02d".format(task.reminderMinute ?: 0)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(onClick = {
                            cancelTaskReminder(context, task.id)
                            deleteTask(db, task.id)
                            onTasksChanged()
                        }) {
                            Text("✕")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "پیشرفت امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${(progress * 100).toInt()}٪")

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun CalendarScreen() {
    val today = LocalDate.now()
    val (jy, jm, jd) = toJalali(today.year, today.monthValue, today.dayOfMonth)
    val monthName = persianMonths[jm - 1]
    var selectedDay by remember { mutableStateOf(jd) }

    val firstDayGregorian = today.minusDays((jd - 1).toLong())
    var daysInMonth = 0
    var cursor = firstDayGregorian
    while (true) {
        val (_, mm, _) = toJalali(cursor.year, cursor.monthValue, cursor.dayOfMonth)
        if (mm != jm) break
        daysInMonth++
        cursor = cursor.plusDays(1)
    }
    val firstWeekdayIndex = firstDayGregorian.dayOfWeek.value % 7
    val leadingBlanks = (firstWeekdayIndex + 1) % 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "تقویم 📅", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$monthName $jy", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    persianWeekdaysShort.forEach { d ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(text = d, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val cells = mutableListOf<Int?>()
                repeat(leadingBlanks) { cells.add(null) }
                for (day in 1..daysInMonth) cells.add(day)
                while (cells.size % 7 != 0) cells.add(null)

                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(3.dp)
                                    .height(40.dp)
                                    .then(
                                        if (day != null) Modifier.clickable { selectedDay = day } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isToday = day == jd
                                    val isSelected = day == selectedDay && !isToday
                                    when {
                                        isToday -> {
                                            Card(shape = RoundedCornerShape(10.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = day.toString(), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        isSelected -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = day.toString())
                                            }
                                        }
                                        else -> {
                                            Text(text = day.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "روز انتخاب‌شده: $selectedDay $monthName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun GoalsScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onGoalsChanged: () -> Unit) {
    val context = LocalContext.current
    val goals = remember(version) { loadGoals(db) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "اهداف 🎯", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (goals.isEmpty()) {
            Text(text = "هنوز هدفی ثبت نشده")
        }
        goals.forEach { goal ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "🎯 ${goal.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (goal.recurrence == "monthly") {
                                Text(text = "🔁 هدف ماهانه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        TextButton(onClick = {
                            deleteGoal(db, goal.id)
                            onGoalsChanged()
                        }) {
                            Text("✕")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { goal.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(goal.progress * 100).toInt()}٪  •  " + (if (goal.targetDate != null) "${goal.daysLeft} روز تا هدف" else "بدون تاریخ هدف"),
                            fontSize = 13.sp
                        )
                        Row {
                            TextButton(onClick = {
                                updateGoalProgress(db, goal.id, goal.progress - 0.1f)
                                onGoalsChanged()
                            }) { Text("−") }
                            TextButton(onClick = {
                                updateGoalProgress(db, goal.id, goal.progress + 0.1f)
                                onGoalsChanged()
                            }) { Text("+") }
                        }
                    }
                    TextButton(onClick = {
                        val base = goal.targetDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                        DatePickerDialog(context, { _, y, m, d ->
                            val picked = LocalDate.of(y, m + 1, d)
                            setGoalTargetDate(db, goal.id, picked.toString())
                            onGoalsChanged()
                        }, base.year, base.monthValue - 1, base.dayOfMonth).show()
                    }) {
                        Text("📅 " + (goal.targetDate ?: "تعیین تاریخ هدف"))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onIdeasChanged: () -> Unit) {
    val ideas = remember(version) { loadIdeas(db) }
    var newIdea by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (text != null) {
                newIdea = text
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "ایده‌های من 💡", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newIdea,
                    onValueChange = { newIdea = it },
                    placeholder = { Text("💭 ایده‌ای که الان به ذهنت رسید را بنویس...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                        }
                    }) {
                        Text("🎙️ ضبط سریع")
                    }
                    Button(onClick = {
                        if (newIdea.isNotBlank()) {
                            insertIdea(db, newIdea)
                            onIdeasChanged()
                            newIdea = ""
                        }
                    }) {
                        Text("ثبت")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (ideas.isEmpty()) {
            Text(text = "هنوز ایده‌ای ثبت نشده")
        }
        ideas.forEach { idea ->
            key(idea.id) {
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            deleteIdea(db, idea.id)
                            onIdeasChanged()
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text("🗑️ حذف", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "💡 ${idea.title}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${idea.date}   ${idea.tag}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HabitsScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onHabitsChanged: () -> Unit, onBack: () -> Unit) {
    val habits = remember(version) { loadHabits(db) }
    val dayLabels = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "عادت‌های امروز 🔥", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        habits.forEach { habit ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = habit.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(onClick = { deleteHabit(db, habit.id); onHabitsChanged() }) { Text("✕") }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayLabels.forEachIndexed { index, label ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = label.take(1), fontSize = 11.sp)
                                Checkbox(
                                    checked = habit.days[index],
                                    onCheckedChange = { checked ->
                                        updateHabitDay(db, habit.id, index, checked)
                                        onHabitsChanged()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "🔥 " + habit.streak.toString() + " روز متوالی", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun NotesScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onNotesChanged: () -> Unit, onBack: () -> Unit) {
    val notes = remember(version) { loadNotes(db) }
    var newNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "یادداشت‌ها 📝", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newNote,
                    onValueChange = { newNote = it },
                    placeholder = { Text("یادداشت جدید...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (newNote.isNotBlank()) {
                            insertNote(db, newNote)
                            onNotesChanged()
                            newNote = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ثبت یادداشت")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notes.isEmpty()) {
            Text(text = "هنوز یادداشتی ثبت نشده")
        }
        notes.forEach { note ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = note.text, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        deleteNote(db, note.id)
                        onNotesChanged()
                    }) {
                        Text("✕")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ReportScreen(db: android.database.sqlite.SQLiteDatabase, onBack: () -> Unit) {
    val tasks = loadTasks(db)
    val goals = loadGoals(db)
    val habits = loadHabits(db)
    val ideas = loadIdeas(db)

    val doneTasks = tasks.count { it.done }
    val avgGoalProgress = if (goals.isNotEmpty()) (goals.map { it.progress }.average() * 100).toInt() else 0
    val bestStreak = habits.maxOfOrNull { it.streak } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "گزارش 📊", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "کارها", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "$doneTasks از ${tasks.size} کار انجام شده")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "اهداف", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${goals.size} هدف ثبت‌شده، میانگین پیشرفت $avgGoalProgress٪")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "عادت‌ها", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "بهترین رکورد: $bestStreak روز متوالی")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "ایده‌ها", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${ideas.size} ایده ثبت‌شده")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SettingsScreen(
    darkModeSetting: Int,
    onDarkModeChange: (Int) -> Unit,
    onResetAll: () -> Unit,
    onBack: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "تنظیمات ⚙️", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "ظاهر برنامه", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf("خودکار" to 0, "روشن" to 1, "تاریک" to 2)
            options.forEach { (label, value) ->
                val selected = darkModeSetting == value
                Button(
                    onClick = { onDarkModeChange(value) },
                    colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "داده‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { showConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("پاک کردن همه اطلاعات")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("پاک کردن اطلاعات") },
            text = { Text("همه کارها، ایده‌ها، اهداف و یادداشت‌ها پاک می‌شوند. مطمئنی؟") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAll()
                    showConfirm = false
                }) {
                    Text("بله، پاک کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun MoreScreen(
    db: android.database.sqlite.SQLiteDatabase,
    habitsVersion: Int,
    onHabitsChanged: () -> Unit,
    notesVersion: Int,
    onNotesChanged: () -> Unit,
    darkModeSetting: Int,
    onDarkModeChange: (Int) -> Unit,
    onResetAll: () -> Unit
) {
    var openSection by remember { mutableStateOf<String?>(null) }

    when (openSection) {
        "عادت‌ها" -> {
            HabitsScreen(db, habitsVersion, onHabitsChanged, onBack = { openSection = null })
            return
        }
        "گزارش" -> {
            ReportScreen(db, onBack = { openSection = null })
            return
        }
        "یادداشت‌ها" -> {
            NotesScreen(db, notesVersion, onNotesChanged, onBack = { openSection = null })
            return
        }
        "تنظیمات" -> {
            SettingsScreen(darkModeSetting, onDarkModeChange, onResetAll, onBack = { openSection = null })
            return
        }
    }

    val menuItems = listOf("🔥 عادت‌ها", "📊 گزارش", "📝 یادداشت‌ها", "⚙️ تنظیمات")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "بیشتر ☰", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        menuItems.forEach { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openSection = item.substringAfter(" ") }
            ) {
                Text(
                    text = item,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    onDismiss: () -> Unit,
    onAddTask: (String, String, Int?, Int?) -> Unit,
    onAddIdea: (String) -> Unit,
    onAddGoal: (String, String) -> Unit,
    onAddNote: (String) -> Unit,
    onAddSchedule: (String, String) -> Unit,
    onAddHabit: (String) -> Unit
) {
    var step by remember { mutableStateOf("menu") }
    var inputText by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableStateOf(9) }
    var reminderMinute by remember { mutableStateOf(0) }
    var recurring by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (step) {
                "menu" -> {
                    Text(
                        text = "چه چیزی می‌خواهی اضافه کنی؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val options = listOf(
                        "🕐 برنامه" to "schedule",
                        "📋 کار" to "task",
                        "💡 ایده" to "idea",
                        "🎯 هدف" to "goal",
                        "📝 یادداشت" to "note",
                        "🔥 عادت" to "habit"
                    )
                    options.forEach { (label, target) ->
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { step = target }
                                .padding(vertical = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                "soon" -> {
                    Text(text = "این بخش به‌زودی اضافه می‌شود.", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { step = "menu" }) {
                        Text("بازگشت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                "task" -> {
                    Text(text = "افزودن کار جدید", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("متن را بنویس...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reminderEnabled,
                            onClick = {
                                TimePickerDialog(
                                    ctx,
                                    { _, h, m -> reminderHour = h; reminderMinute = m; reminderEnabled = true },
                                    reminderHour, reminderMinute, true
                                ).show()
                            },
                            label = {
                                Text(
                                    if (reminderEnabled)
                                        "⏰ ${"%02d".format(reminderHour)}:${"%02d".format(reminderMinute)}"
                                    else "⏰ یادآوری"
                                )
                            }
                        )
                        FilterChip(
                            selected = recurring,
                            onClick = { recurring = !recurring },
                            label = { Text("🔁 تکرار ماهانه") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onAddTask(
                                    inputText,
                                    if (recurring) "monthly" else "none",
                                    if (reminderEnabled) reminderHour else null,
                                    if (reminderEnabled) reminderMinute else null
                                )
                                inputText = ""
                                reminderEnabled = false
                                recurring = false
                                step = "menu"
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                "goal" -> {
                    Text(text = "افزودن هدف جدید", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("متن را بنویس...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilterChip(
                        selected = recurring,
                        onClick = { recurring = !recurring },
                        label = { Text("🔁 هدف ماهانه (تکرارشونده)") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onAddGoal(inputText, if (recurring) "monthly" else "none")
                                inputText = ""
                                recurring = false
                                step = "menu"
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                else -> {
                    val title = when (step) {
                        "idea" -> "افزودن ایده جدید"
                        "note" -> "افزودن یادداشت جدید"
                        "schedule" -> "افزودن برنامه (مثال: ۱۸:۰۰ ورزش)"
                        "habit" -> "افزودن عادت جدید"
                        else -> "افزودن"
                    }
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("متن را بنویس...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                when (step) {
                                    "idea" -> onAddIdea(inputText)
                                    "note" -> onAddNote(inputText)
                                    "schedule" -> {
                                        val parts = inputText.trim().split(" ", limit = 2)
                                        onAddSchedule(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
                                    }
                                    "habit" -> onAddHabit(inputText)
                                }
                                inputText = ""
                                step = "menu"
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun RoozemanBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("خانه", Icons.Default.Home, 0),
        Triple("تقویم", Icons.Default.DateRange, 1),
        Triple("اهداف", Icons.Default.Star, 2),
        Triple("ایده‌ها", Icons.Default.Info, 3),
        Triple("بیشتر", Icons.Default.Menu, 4)
    )
    NavigationBar {
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
MAINEOF

echo "✅ همه‌ی فایل‌ها با موفقیت و یکجا بازنویسی شدند."
echo "حالا: git add . && git commit -m \"full clean sync of all files\" && git push"
