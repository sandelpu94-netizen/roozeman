package com.roozeman.app

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false
)

@Entity(tableName = "ideas")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val tag: String
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val progress: Float,
    val daysLeft: Int
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAll(): Flow<List<TaskEntity>>
    @Insert
    suspend fun insert(task: TaskEntity)
    @Update
    suspend fun update(task: TaskEntity)
}

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY id DESC")
    fun getAll(): Flow<List<IdeaEntity>>
    @Insert
    suspend fun insert(idea: IdeaEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun getAll(): Flow<List<GoalEntity>>
    @Insert
    suspend fun insert(goal: GoalEntity)
}

@Database(entities = [TaskEntity::class, IdeaEntity::class, GoalEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun ideaDao(): IdeaDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roozeman.db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
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
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
