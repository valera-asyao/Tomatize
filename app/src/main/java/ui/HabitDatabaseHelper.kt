package ui

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar
class HabitDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "habits.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_HABITS = "habits"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_CREATED_AT = "created_at"

        private const val COLUMN_STREAK_COUNT = "streak_count"

        private const val COLUMN_LAST_COMPLETED = "last_completed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_HABITS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_STREAK_COUNT INTEGER DEFAULT 0,
                $COLUMN_LAST_COMPLETED INTEGER,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HABITS")
        onCreate(db)
    }

    fun addHabit(habit: Habit): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, habit.name)
            put(COLUMN_DESCRIPTION, habit.description)
            put(COLUMN_TYPE, habit.type.name)
            put(COLUMN_STREAK_COUNT, habit.streakCount)
            put(COLUMN_LAST_COMPLETED, habit.lastCompleted)
            put(COLUMN_CREATED_AT, habit.createdAt)
        }
        val id = db.insert(TABLE_HABITS, null, values)
        db.close()
        return id
    }

    fun getAllHabits(): List<Habit> {
        val habits = mutableListOf<Habit>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABITS,
            null, null, null, null, null,
            "$COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val habit = Habit(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    type = HabitType.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE))),
                    streakCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_STREAK_COUNT)),
                    lastCompleted = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED)))
                        null else it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                )
                habits.add(habit)
            }
        }
        db.close()
        return habits
    }

    fun updateHabitStreak(habitId: Long, streakCount: Int, lastCompleted: Long?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_STREAK_COUNT, streakCount)
            put(COLUMN_LAST_COMPLETED, lastCompleted)
        }
        val result = db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(habitId.toString()))
        db.close()
        return result > 0
    }

    fun completeHabit(habitId: Long): Boolean {
        val habit = getHabitById(habitId)
        habit?.let {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply { timeInMillis = now }

            // Проверяем, выполнялась ли привычка сегодня
            val lastCompleted = it.lastCompleted
            val shouldIncrementStreak = if (lastCompleted != null) {
                val lastDate = Calendar.getInstance().apply { timeInMillis = lastCompleted }
                !isSameDay(lastDate, calendar)
            } else {
                true
            }

            val newStreak = if (shouldIncrementStreak) it.streakCount + 1 else it.streakCount

            return updateHabitStreak(habitId, newStreak, now)
        }
        return false
    }

    fun resetHabitStreak(habitId: Long): Boolean {
        return updateHabitStreak(habitId, 0, null)
    }

    private fun getHabitById(habitId: Long): Habit? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABITS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(habitId.toString()),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                Habit(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    type = HabitType.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE))),
                    streakCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_STREAK_COUNT)),
                    lastCompleted = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED)))
                        null else it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                )
            } else {
                null
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}