package ui

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

data class HabitStatistics(
    val totalDays: Int,
    val completedDays: Int,
    val completionRate: Int,
    val currentStreak: Int,
    val recordStreak: Int,
    val completions: List<Long>
)

class HabitDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "habits.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_HABITS = "habits"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_STREAK_COUNT = "streak_count"
        private const val COLUMN_LAST_COMPLETED = "last_completed"
        private const val COLUMN_CREATED_AT = "created_at"

        private const val TABLE_HABIT_COMPLETIONS = "habit_completions"
        private const val COLUMN_COMPLETION_ID = "completion_id"
        private const val COLUMN_HABIT_ID_FK = "habit_id_fk"
        private const val COLUMN_COMPLETION_DATE = "completion_date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_HABITS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_STREAK_COUNT INTEGER DEFAULT 0,
                $COLUMN_LAST_COMPLETED INTEGER,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $TABLE_HABIT_COMPLETIONS (
                $COLUMN_COMPLETION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HABIT_ID_FK INTEGER NOT NULL,
                $COLUMN_COMPLETION_DATE INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_HABIT_ID_FK) REFERENCES $TABLE_HABITS($COLUMN_ID)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE $TABLE_HABIT_COMPLETIONS ($COLUMN_COMPLETION_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_HABIT_ID_FK INTEGER NOT NULL, $COLUMN_COMPLETION_DATE INTEGER NOT NULL, FOREIGN KEY ($COLUMN_HABIT_ID_FK) REFERENCES $TABLE_HABITS($COLUMN_ID))")
        }
    }

    fun isDataBaseEmpty(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_HABITS", null)
        var isEmpty = true
        cursor.use {
            if (it.moveToFirst()) {
                isEmpty = it.getInt(0) == 0
            }
        }
        return isEmpty
    }

    fun addHabit(habit: Habit): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, habit.name)
            put(COLUMN_DESCRIPTION, habit.description)
            put(COLUMN_TYPE, habit.type.name)
            put(COLUMN_STREAK_COUNT, 0)
            put(COLUMN_LAST_COMPLETED, null as Long?)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        return db.insert(TABLE_HABITS, null, values)
    }

    fun getAllHabits(): List<Habit> {
        updateStreaks()
        val habits = mutableListOf<Habit>()
        val db = readableDatabase
        val cursor = db.query(TABLE_HABITS, null, null, null, null, null, "$COLUMN_CREATED_AT DESC")
        cursor.use {
            while (it.moveToNext()) {
                habits.add(Habit(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    type = HabitType.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE))),
                    streakCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_STREAK_COUNT)),
                    lastCompleted = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED))) null else it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                ))
            }
        }
        return habits
    }

    private fun updateStreaks() {
        val db = writableDatabase
        val cursor = db.query(TABLE_HABITS, null, null, null, null, null, null)
        val now = Calendar.getInstance()
        
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val type = HabitType.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE)))
                val lastFailureLong = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED))) null else it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED))
                val createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                
                if (type == HabitType.GOOD) {
                    if (lastFailureLong != null) {
                        val lastCal = Calendar.getInstance().apply { timeInMillis = lastFailureLong }
                        if (!isSameDay(lastCal, now) && !isYesterday(lastCal, now)) {
                            db.execSQL("UPDATE $TABLE_HABITS SET $COLUMN_STREAK_COUNT = 0 WHERE $COLUMN_ID = $id")
                        }
                    }
                } else {
                    // Для плохих привычек стрик = дни с момента создания или последнего срыва
                    val baseDate = lastFailureLong ?: createdAt
                    val diff = now.timeInMillis - baseDate
                    val daysHeld = (diff / (1000 * 60 * 60 * 24)).toInt()
                    db.execSQL("UPDATE $TABLE_HABITS SET $COLUMN_STREAK_COUNT = $daysHeld WHERE $COLUMN_ID = $id")
                }
            }
        }
    }

    fun completeHabit(habitId: Long): Boolean {
        val habit = getHabitById(habitId) ?: return false
        if (habit.type == HabitType.BAD) return false

        val now = System.currentTimeMillis()
        val last = habit.lastCompleted
        if (last != null && isSameDay(Calendar.getInstance().apply { timeInMillis = last }, Calendar.getInstance())) return false

        val db = writableDatabase
        val newStreak = habit.streakCount + 1
        val values = ContentValues().apply {
            put(COLUMN_STREAK_COUNT, newStreak)
            put(COLUMN_LAST_COMPLETED, now)
        }
        db.insert(TABLE_HABIT_COMPLETIONS, null, ContentValues().apply {
            put(COLUMN_HABIT_ID_FK, habitId)
            put(COLUMN_COMPLETION_DATE, now)
        })
        return db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(habitId.toString())) > 0
    }

    fun recordFailure(habitId: Long): Boolean {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COLUMN_STREAK_COUNT, 0)
            put(COLUMN_LAST_COMPLETED, now)
        }
        db.insert(TABLE_HABIT_COMPLETIONS, null, ContentValues().apply {
            put(COLUMN_HABIT_ID_FK, habitId)
            put(COLUMN_COMPLETION_DATE, now)
        })
        return db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(habitId.toString())) > 0
    }

    fun undoCompleteHabit(habitId: Long): Boolean {
        val habit = getHabitById(habitId) ?: return false
        val last = habit.lastCompleted ?: return false
        if (!isSameDay(Calendar.getInstance().apply { timeInMillis = last }, Calendar.getInstance())) return false

        val db = writableDatabase
        db.delete(TABLE_HABIT_COMPLETIONS, "$COLUMN_HABIT_ID_FK = ? AND $COLUMN_COMPLETION_DATE = ?", arrayOf(habitId.toString(), last.toString()))
        
        val cursor = db.rawQuery("SELECT $COLUMN_COMPLETION_DATE FROM $TABLE_HABIT_COMPLETIONS WHERE $COLUMN_HABIT_ID_FK = $habitId ORDER BY $COLUMN_COMPLETION_DATE DESC LIMIT 1", null)
        var prevDate: Long? = null
        if (cursor.moveToFirst()) prevDate = cursor.getLong(0)
        cursor.close()

        val values = ContentValues().apply {
            put(COLUMN_STREAK_COUNT, maxOf(0, habit.streakCount - 1))
            put(COLUMN_LAST_COMPLETED, prevDate as Long?)
        }
        return db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(habitId.toString())) > 0
    }

    fun getHabitById(id: Long): Habit? {
        val db = readableDatabase
        val cursor = db.query(TABLE_HABITS, null, "$COLUMN_ID = ?", arrayOf(id.toString()), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) Habit(
                id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                type = HabitType.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_TYPE))),
                streakCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_STREAK_COUNT)),
                lastCompleted = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED))) null else it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED)),
                createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
            ) else null
        }
    }

    fun deleteHabit(id: Long): Boolean {
        val db = writableDatabase
        db.delete(TABLE_HABIT_COMPLETIONS, "$COLUMN_HABIT_ID_FK = ?", arrayOf(id.toString()))
        return db.delete(TABLE_HABITS, "$COLUMN_ID = ?", arrayOf(id.toString())) > 0
    }

    fun getHabitStatistics(habitId: Long): HabitStatistics {
        val completions = mutableListOf<Long>()
        val db = readableDatabase
        db.rawQuery("SELECT $COLUMN_COMPLETION_DATE FROM $TABLE_HABIT_COMPLETIONS WHERE $COLUMN_HABIT_ID_FK = $habitId", null).use {
            while (it.moveToNext()) completions.add(it.getLong(0))
        }
        val habit = getHabitById(habitId) ?: return HabitStatistics(0,0,0,0,0, emptyList())
        return HabitStatistics(calculateTotalDays(habit.createdAt), completions.size, 0, habit.streakCount, 0, completions)
    }

    private fun calculateTotalDays(createdAt: Long): Int {
        val diff = System.currentTimeMillis() - createdAt
        return (diff / (1000 * 60 * 60 * 24)).toInt() + 1
    }

    private fun isSameDay(c1: Calendar, c2: Calendar) = c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    private fun isYesterday(last: Calendar, now: Calendar): Boolean {
        val yest = now.clone() as Calendar
        yest.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(last, yest)
    }

    fun hasUncompletedHabitsToday(): Boolean {
        val habits = getAllHabits()
        val now = Calendar.getInstance()
        return habits.any { habit ->
            if (habit.type == HabitType.GOOD) {
                val last = habit.lastCompleted
                last == null || !isSameDay(Calendar.getInstance().apply { timeInMillis = last }, now)
            } else false
        }
    }
}