package com.roozeman.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class TaskItem(val id: Long, val title: String, val done: Boolean)
data class IdeaItem(val id: Long, val title: String, val date: String, val tag: String)
data class GoalItem(val id: Long, val title: String, val progress: Float, val daysLeft: Int)
data class HabitItem(val id: Long, val title: String, val days: List<Boolean>, val streak: Int)
data class NoteItem(val id: Long, val text: String)

class DbHelper(context: Context) : SQLiteOpenHelper(context, "roozeman.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, done INTEGER)")
        db.execSQL("CREATE TABLE ideas (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, date TEXT, tag TEXT)")
        db.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, progress REAL, daysLeft INTEGER)")
        db.execSQL("CREATE TABLE habits (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, d0 INTEGER, d1 INTEGER, d2 INTEGER, d3 INTEGER, d4 INTEGER, d5 INTEGER, d6 INTEGER, streak INTEGER)")
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT)")

        db.execSQL("INSERT INTO tasks (title, done) VALUES ('انجام کاری که امروز مهم‌تر از همه است', 0)")
        db.execSQL("INSERT INTO tasks (title, done) VALUES ('چک کردن ایمیل‌ها', 1)")
        db.execSQL("INSERT INTO tasks (title, done) VALUES ('۳۰ دقیقه پیاده‌روی', 1)")
        db.execSQL("INSERT INTO tasks (title, done) VALUES ('مطالعه ۲۰ دقیقه', 0)")

        db.execSQL("INSERT INTO goals (title, progress, daysLeft) VALUES ('یادگیری زبان انگلیسی', 0.6, 12)")
        db.execSQL("INSERT INTO goals (title, progress, daysLeft) VALUES ('ورزش منظم', 0.3, 45)")
        db.execSQL("INSERT INTO goals (title, progress, daysLeft) VALUES ('مطالعه ۱۲ کتاب امسال', 0.4, 90)")

        db.execSQL("INSERT INTO ideas (title, date, tag) VALUES ('طراحی یک محصول جدید', '۱۳ شهریور', '⭐ مهم')")
        db.execSQL("INSERT INTO ideas (title, date, tag) VALUES ('پیشنهاد ویژگی جدید برای اپ', '۱۰ شهریور', '💡 ایده')")

        db.execSQL("INSERT INTO habits (title, d0, d1, d2, d3, d4, d5, d6, streak) VALUES ('ورزش', 1, 1, 0, 1, 0, 0, 0, 12)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, d0 INTEGER, d1 INTEGER, d2 INTEGER, d3 INTEGER, d4 INTEGER, d5 INTEGER, d6 INTEGER, streak INTEGER)")
            val cursor = db.rawQuery("SELECT COUNT(*) FROM habits", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            if (count == 0) {
                db.execSQL("INSERT INTO habits (title, d0, d1, d2, d3, d4, d5, d6, streak) VALUES ('ورزش', 1, 1, 0, 1, 0, 0, 0, 12)")
            }
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT)")
        }
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

fun deleteGoal(db: SQLiteDatabase, id: Long) {
    db.delete("goals", "id = ?", arrayOf(id.toString()))
}

fun loadHabits(db: SQLiteDatabase): List<HabitItem> {
    val list = mutableListOf<HabitItem>()
    val cursor = db.rawQuery("SELECT id, title, d0, d1, d2, d3, d4, d5, d6, streak FROM habits ORDER BY id ASC", null)
    while (cursor.moveToNext()) {
        val days = (0..6).map { cursor.getInt(2 + it) == 1 }
        list.add(HabitItem(cursor.getLong(0), cursor.getString(1), days, cursor.getInt(9)))
    }
    cursor.close()
    return list
}

fun updateHabitDay(db: SQLiteDatabase, id: Long, dayIndex: Int, value: Boolean) {
    val col = "d" + dayIndex.toString()
    db.execSQL("UPDATE habits SET " + col + " = ? WHERE id = ?", arrayOf(if (value) 1 else 0, id))
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

fun resetAllData(db: SQLiteDatabase) {
    db.execSQL("DELETE FROM tasks")
    db.execSQL("DELETE FROM ideas")
    db.execSQL("DELETE FROM goals")
    db.execSQL("DELETE FROM notes")
    db.execSQL("UPDATE habits SET d0=0, d1=0, d2=0, d3=0, d4=0, d5=0, d6=0, streak=0")
}
