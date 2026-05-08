package ui

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
        private const val DATABASE_VERSION = 4
        private const val TABLE_HABITS = "habits"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_BAD_DIFFICULTY = "bad_difficulty"
        private const val COLUMN_STREAK_COUNT = "streak_count"
        private const val COLUMN_RECORD_STREAK = "record_streak"
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
                $COLUMN_BAD_DIFFICULTY INTEGER NOT NULL DEFAULT $DEFAULT_BAD_DIFFICULTY,
                $COLUMN_STREAK_COUNT INTEGER DEFAULT 0,
                $COLUMN_RECORD_STREAK INTEGER DEFAULT 0,
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
        if (oldVersion < 3) {
            addColumnIfNeeded(db, COLUMN_RECORD_STREAK, "ALTER TABLE $TABLE_HABITS ADD COLUMN $COLUMN_RECORD_STREAK INTEGER DEFAULT 0")
        }
        if (oldVersion < 4) {
            addColumnIfNeeded(db, COLUMN_RECORD_STREAK, "ALTER TABLE $TABLE_HABITS ADD COLUMN $COLUMN_RECORD_STREAK INTEGER DEFAULT 0")
            addColumnIfNeeded(db, COLUMN_BAD_DIFFICULTY, "ALTER TABLE $TABLE_HABITS ADD COLUMN $COLUMN_BAD_DIFFICULTY INTEGER NOT NULL DEFAULT $DEFAULT_BAD_DIFFICULTY")
        }
    }

    private fun addColumnIfNeeded(db: SQLiteDatabase, columnName: String, sql: String) {
        var exists = false
        val cursor = db.rawQuery("PRAGMA table_info($TABLE_HABITS)", null)
        cursor.use {
            while (it.moveToNext()) {
                if (it.getString(it.getColumnIndexOrThrow("name")) == columnName) {
                    exists = true
                }
            }
        }
        if (!exists) {
            db.execSQL(sql)
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

    fun existsByName(name: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_HABITS WHERE $COLUMN_NAME = ? LIMIT 1", arrayOf(name))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun existsByNameExceptId(name: String, excludedHabitId: Long): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_HABITS WHERE $COLUMN_NAME = ? AND $COLUMN_ID != ? LIMIT 1",
            arrayOf(name, excludedHabitId.toString())
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun addHabit(habit: Habit): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, habit.name)
            put(COLUMN_DESCRIPTION, habit.description)
            put(COLUMN_TYPE, habit.type.name)
            put(COLUMN_BAD_DIFFICULTY, normalizeBadDifficulty(habit.badDifficulty))
            put(COLUMN_STREAK_COUNT, 0)
            put(COLUMN_RECORD_STREAK, 0)
            put(COLUMN_LAST_COMPLETED, null as Long?)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        return db.insert(TABLE_HABITS, null, values)
    }

    private fun readBadDifficulty(cursor: Cursor): Int {
        val index = cursor.getColumnIndex(COLUMN_BAD_DIFFICULTY)
        if (index == -1 || cursor.isNull(index)) {
            return DEFAULT_BAD_DIFFICULTY
        }
        return normalizeBadDifficulty(cursor.getInt(index))
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
                    badDifficulty = readBadDifficulty(it),
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
                val lastDateLong = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED))) null else it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED))
                val createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                var currentStreak = it.getInt(it.getColumnIndexOrThrow(COLUMN_STREAK_COUNT))
                var recordStreak = it.getInt(it.getColumnIndexOrThrow(COLUMN_RECORD_STREAK))
                
                if (type == HabitType.GOOD) {
                    if (lastDateLong != null) {
                        val lastCal = Calendar.getInstance().apply { timeInMillis = lastDateLong }
                        if (!isSameDay(lastCal, now) && !isYesterday(lastCal, now)) {
                            currentStreak = 0
                        }
                    }
                } else {
                    val baseDate = lastDateLong ?: createdAt
                    val diff = now.timeInMillis - baseDate
                    val fullDays = (diff / (1000 * 60 * 60 * 24)).toInt()
                    
                    val isFailedToday = lastDateLong != null && isSameDay(Calendar.getInstance().apply { timeInMillis = lastDateLong }, now)
                    currentStreak = if (isFailedToday) 0 else fullDays + 1
                }
                
                if (currentStreak > recordStreak) {
                    recordStreak = currentStreak
                }
                
                val values = ContentValues().apply {
                    put(COLUMN_STREAK_COUNT, currentStreak)
                    put(COLUMN_RECORD_STREAK, recordStreak)
                }
                db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
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
        
        // Получаем текущий рекорд из БД напрямую для точности
        val cursor = db.rawQuery("SELECT $COLUMN_RECORD_STREAK FROM $TABLE_HABITS WHERE $COLUMN_ID = $habitId", null)
        var currentRecord = 0
        if (cursor.moveToFirst()) currentRecord = cursor.getInt(0)
        cursor.close()
        
        val newRecord = maxOf(newStreak, currentRecord)

        val values = ContentValues().apply {
            put(COLUMN_STREAK_COUNT, newStreak)
            put(COLUMN_RECORD_STREAK, newRecord)
            put(COLUMN_LAST_COMPLETED, now)
        }
        db.insert(TABLE_HABIT_COMPLETIONS, null, ContentValues().apply {
            put(COLUMN_HABIT_ID_FK, habitId)
            put(COLUMN_COMPLETION_DATE, now)
        })
        return db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(habitId.toString())) > 0
    }

    fun recordFailure(habitId: Long): Boolean {
        val habit = getHabitById(habitId) ?: return false
        val last = habit.lastCompleted
        if (last != null && isSameDay(Calendar.getInstance().apply { timeInMillis = last }, Calendar.getInstance())) return false

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

    fun toggleHabitCompletionOnDate(habitId: Long, dateMillis: Long): Boolean {
        val habit = getHabitById(habitId) ?: return false
        val dayStart = getStartOfDay(dateMillis)
        val createdStart = getStartOfDay(habit.createdAt)
        val todayStart = getStartOfDay(System.currentTimeMillis())

        if (dayStart < createdStart || dayStart > todayStart) {
            return false
        }

        val dayEnd = getEndOfDay(dateMillis)
        val completed = isHabitCompletedOnDate(habitId, dateMillis)
        val db = writableDatabase

        val changed = if (completed) {
            db.delete(
                TABLE_HABIT_COMPLETIONS,
                "$COLUMN_HABIT_ID_FK = ? AND $COLUMN_COMPLETION_DATE BETWEEN ? AND ?",
                arrayOf(habitId.toString(), dayStart.toString(), dayEnd.toString())
            ) > 0
        } else {
            val values = ContentValues().apply {
                put(COLUMN_HABIT_ID_FK, habitId)
                put(COLUMN_COMPLETION_DATE, dayStart)
            }
            db.insert(TABLE_HABIT_COMPLETIONS, null, values) != -1L
        }

        if (!changed) {
            return false
        }

        return updateHabitProgressFromCompletions(habitId)
    }

    fun isHabitCompletedOnDate(habitId: Long, dateMillis: Long): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT_COMPLETIONS,
            arrayOf(COLUMN_COMPLETION_ID),
            "$COLUMN_HABIT_ID_FK = ? AND $COLUMN_COMPLETION_DATE BETWEEN ? AND ?",
            arrayOf(habitId.toString(), getStartOfDay(dateMillis).toString(), getEndOfDay(dateMillis).toString()),
            null, null, null,
            "1"
        )

        val completed = cursor.moveToFirst()
        cursor.close()
        return completed
    }

    private fun updateHabitProgressFromCompletions(habitId: Long): Boolean {
        val habit = getHabitById(habitId) ?: return false
        val completions = getHabitCompletionList(habitId)
        val days = completions.map { getStartOfDay(it) }.distinct().sorted()

        val currentStreak = if (habit.type == HabitType.GOOD) {
            calculateGoodCurrentStreak(days)
        } else {
            calculateBadCurrentStreak(habit.createdAt, days)
        }

        val recordStreak = if (habit.type == HabitType.GOOD) {
            calculateGoodRecordStreak(days)
        } else {
            calculateBadRecordStreak(habit.createdAt, days)
        }

        val values = ContentValues().apply {
            put(COLUMN_STREAK_COUNT, currentStreak)
            put(COLUMN_RECORD_STREAK, recordStreak)
            val lastCompleted = completions.maxOrNull()
            if (lastCompleted == null) {
                putNull(COLUMN_LAST_COMPLETED)
            } else {
                put(COLUMN_LAST_COMPLETED, lastCompleted)
            }
        }

        val db = writableDatabase
        return db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(habitId.toString())) > 0
    }

    private fun getHabitCompletionList(habitId: Long): List<Long> {
        val completions = mutableListOf<Long>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT $COLUMN_COMPLETION_DATE FROM $TABLE_HABIT_COMPLETIONS WHERE $COLUMN_HABIT_ID_FK = ? ORDER BY $COLUMN_COMPLETION_DATE ASC",
            arrayOf(habitId.toString())
        ).use {
            while (it.moveToNext()) {
                completions.add(it.getLong(0))
            }
        }
        return completions
    }

    private fun calculateGoodCurrentStreak(days: List<Long>): Int {
        if (days.isEmpty()) return 0

        val daySet = days.toSet()
        val today = getStartOfDay(System.currentTimeMillis())
        val yesterday = addDays(today, -1)

        var day = when {
            daySet.contains(today) -> today
            daySet.contains(yesterday) -> yesterday
            else -> return 0
        }

        var streak = 0
        while (daySet.contains(day)) {
            streak++
            day = addDays(day, -1)
        }
        return streak
    }

    private fun calculateGoodRecordStreak(days: List<Long>): Int {
        if (days.isEmpty()) return 0

        var best = 1
        var current = 1
        for (i in 1 until days.size) {
            if (days[i] == addDays(days[i - 1], 1)) {
                current++
            } else {
                current = 1
            }
            best = maxOf(best, current)
        }
        return best
    }

    private fun calculateBadCurrentStreak(createdAt: Long, failureDays: List<Long>): Int {
        val createdStart = getStartOfDay(createdAt)
        val failureSet = failureDays.toSet()
        var day = getStartOfDay(System.currentTimeMillis())
        var streak = 0

        while (day >= createdStart && !failureSet.contains(day)) {
            streak++
            day = addDays(day, -1)
        }

        return streak
    }

    private fun calculateBadRecordStreak(createdAt: Long, failureDays: List<Long>): Int {
        val today = getStartOfDay(System.currentTimeMillis())
        val failureSet = failureDays.toSet()
        var day = getStartOfDay(createdAt)
        var best = 0
        var current = 0

        while (day <= today) {
            if (failureSet.contains(day)) {
                current = 0
            } else {
                current++
                best = maxOf(best, current)
            }
            day = addDays(day, 1)
        }

        return best
    }

    private fun getStartOfDay(dateMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfDay(dateMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    private fun addDays(dateMillis: Long, count: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            add(Calendar.DAY_OF_YEAR, count)
        }
        return calendar.timeInMillis
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
                badDifficulty = readBadDifficulty(it),
                createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))
            ) else null
        }
    }

    fun updateHabit(id: Long, name: String, description: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DESCRIPTION, description)
        }
        return db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(id.toString())) > 0
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
        
        val cursor = db.rawQuery("SELECT $COLUMN_STREAK_COUNT, $COLUMN_RECORD_STREAK, $COLUMN_CREATED_AT FROM $TABLE_HABITS WHERE $COLUMN_ID = $habitId", null)
        var currentS = 0
        var recordS = 0
        var created: Long = 0
        if (cursor.moveToFirst()) {
            currentS = cursor.getInt(0)
            recordS = cursor.getInt(1)
            created = cursor.getLong(2)
        }
        cursor.close()

        return HabitStatistics(calculateTotalDays(created), completions.size, 0, currentS, recordS, completions)
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
