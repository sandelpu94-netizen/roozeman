package com.roozeman.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class TaskItem(val id: Long, val title: String, val done: Boolean)
data class IdeaItem(val id: Long, val title: String, val date: String, val tag: String)
data class GoalItem(val id: Long, val title: String, val progress: Float, val daysLeft: Int)

class DbHelper(context: Context) : SQLiteOpenHelper(context, "roozeman.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, done INTEGER)")
        db.execSQL("CREATE TABLE ideas (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, date TEXT, tag TEXT)")
        db.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, progress REAL, daysLeft INTEGER)")

        db.execSQL("INSERT INTO tasks (title, done) VALUES ('انجام کاری که امروز مهم‌تر از همه است', 0)")
        db.execSQL("INSERT INTO tasks (title, done) VALUES ('چک کردن ایمیل‌ها', 1)")
        db.execSQL("INSERT INTO tasks (title, done) VALUES ('۳۰ دقیقه پیاده‌روی', 1)")
        db.execSQL("INSERT INTO tasks (title, done) VALUES ('مطالعه ۲۰ دقیقه', 0)")

        db.execSQL("INSERT INTO goals (title, progress, daysLeft) VALUES ('یادگیری زبان انگلیسی', 0.6, 12)")
        db.execSQL("INSERT INTO goals (title, progress, daysLeft) VALUES ('ورزش منظم', 0.3, 45)")
        db.execSQL("INSERT INTO goals (title, progress, daysLeft) VALUES ('مطالعه ۱۲ کتاب امسال', 0.4, 90)")

        db.execSQL("INSERT INTO ideas (title, date, tag) VALUES ('طراحی یک محصول جدید', '۱۳ شهریور', '⭐ مهم')")
        db.execSQL("INSERT INTO ideas (title, date, tag) VALUES ('پیشنهاد ویژگی جدید برای اپ', '۱۰ شهریور', '💡 ایده')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}

fun loadTasks(db: SQLiteDatabase): List<TaskItem> {
    val list = mutableListOf<TaskItem>()
    val cursor = db.rawQuery("SELECT id, title, done FROM tasks ORDER BY id ASC", null)
    while (cursor.moveToNext()) {
        list.add(TaskItem(cursor.getLong(0), cursor.getString(1), cursor.getInt(2) == 1))
    }
    cursor.close()
    return list
}

fun insertTask(db: SQLiteDatabase, title: String) {
    val values = ContentValues()
    values.put("title", title)
    values.put("done", 0)
    db.insert("tasks", null, values)
}

fun updateTaskDone(db: SQLiteDatabase, id: Long, done: Boolean) {
    val values = ContentValues()
    values.put("done", if (done) 1 else 0)
    db.update("tasks", values, "id = ?", arrayOf(id.toString()))
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

fun loadGoals(db: SQLiteDatabase): List<GoalItem> {
    val list = mutableListOf<GoalItem>()
    val cursor = db.rawQuery("SELECT id, title, progress, daysLeft FROM goals ORDER BY id ASC", null)
    while (cursor.moveToNext()) {
        list.add(GoalItem(cursor.getLong(0), cursor.getString(1), cursor.getFloat(2), cursor.getInt(3)))
    }
    cursor.close()
    return list
}

fun insertGoal(db: SQLiteDatabase, title: String) {
    val values = ContentValues()
    values.put("title", title)
    values.put("progress", 0f)
    values.put("daysLeft", 0)
    db.insert("goals", null, values)
}
