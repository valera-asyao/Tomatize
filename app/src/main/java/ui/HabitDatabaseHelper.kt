package ui

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

// Модель для статистики
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
        private const val COLUMN_CREATED_AT = "created_at"

        private const val COLUMN_STREAK_COUNT = "streak_count"

        private const val COLUMN_LAST_COMPLETED = "last_completed"
        private const val TABLE_HABIT_COMPLETIONS = "habit_completions"
        private const val COLUMN_COMPLETION_ID = "completion_id"
        private const val COLUMN_HABIT_ID_FK = "habit_id_fk"
        private const val COLUMN_COMPLETION_DATE = "completion_date"
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

        val createCompletionsTable = """
            CREATE TABLE $TABLE_HABIT_COMPLETIONS (
                $COLUMN_COMPLETION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HABIT_ID_FK INTEGER NOT NULL,
                $COLUMN_COMPLETION_DATE INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_HABIT_ID_FK) REFERENCES $TABLE_HABITS($COLUMN_ID)
            )
        """.trimIndent()
        db.execSQL(createCompletionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createCompletionsTable = """
            CREATE TABLE $TABLE_HABIT_COMPLETIONS (
                $COLUMN_COMPLETION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HABIT_ID_FK INTEGER NOT NULL,
                $COLUMN_COMPLETION_DATE INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_HABIT_ID_FK) REFERENCES $TABLE_HABITS($COLUMN_ID)
            )
        """.trimIndent()
            db.execSQL(createCompletionsTable)
            migrateExistingCompletions(db)
        }
    }

    private fun migrateExistingCompletions(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT $COLUMN_ID, $COLUMN_LAST_COMPLETED FROM $TABLE_HABITS WHERE $COLUMN_LAST_COMPLETED IS NOT NULL", null)

        cursor.use {
            while (it.moveToNext()) {
                val habitId = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val lastCompleted = it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_COMPLETED))

                val values = ContentValues().apply {
                    put(COLUMN_HABIT_ID_FK, habitId)
                    put(COLUMN_COMPLETION_DATE, lastCompleted)
                }
                db.insert(TABLE_HABIT_COMPLETIONS, null, values)
            }
        }
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

    fun addHabitCompletion(habitId: Long, date: Long = System.currentTimeMillis()): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_HABIT_ID_FK, habitId)
            put(COLUMN_COMPLETION_DATE, date)
        }
        val id = db.insert(TABLE_HABIT_COMPLETIONS, null, values)
        db.close()
        return id
    }

    // Проверяет и сбрасывает стрики, если они просрочены
    private fun checkAndResetExpiredStreaks() {
        val habits = getAllHabitsRaw() // Получаем без рекурсии
        val now = Calendar.getInstance()
        
        for (habit in habits) {
            val lastCompleted = habit.lastCompleted
            if (lastCompleted != null && habit.streakCount > 0) {
                val lastDate = Calendar.getInstance().apply { timeInMillis = lastCompleted }
                
                // Стрик протух, если сегодня не было выполнения И вчера не было выполнения
                val completedToday = isSameDay(lastDate, now)
                val completedYesterday = isYesterday(lastDate, now)
                
                if (!completedToday && !completedYesterday) {
                    updateHabitStreak(habit.id, 0, habit.lastCompleted)
                }
            }
        }
    }

    // Внутренний метод для получения списка без вызова проверки стриков (чтобы избежать рекурсии)
    private fun getAllHabitsRaw(): List<Habit> {
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
        return habits
    }

    fun getAllHabits(): List<Habit> {
        checkAndResetExpiredStreaks() // Проверяем стрики перед выдачей списка
        return getAllHabitsRaw()
    }

    fun getHabitCompletions(habitId: Long): List<Long> {
        val completions = mutableListOf<Long>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT_COMPLETIONS,
            arrayOf(COLUMN_COMPLETION_DATE),
            "$COLUMN_HABIT_ID_FK = ?",
            arrayOf(habitId.toString()),
            null, null,
            "$COLUMN_COMPLETION_DATE ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                completions.add(it.getLong(it.getColumnIndexOrThrow(COLUMN_COMPLETION_DATE)))
            }
        }
        db.close()
        return completions
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

    fun getHabitCompletionsInRange(habitId: Long, startDate: Long, endDate: Long): List<Long> {
        val completions = mutableListOf<Long>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT_COMPLETIONS,
            arrayOf(COLUMN_COMPLETION_DATE),
            "$COLUMN_HABIT_ID_FK = ? AND $COLUMN_COMPLETION_DATE BETWEEN ? AND ?",
            arrayOf(habitId.toString(), startDate.toString(), endDate.toString()),
            null, null,
            "$COLUMN_COMPLETION_DATE ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                completions.add(it.getLong(it.getColumnIndexOrThrow(COLUMN_COMPLETION_DATE)))
            }
        }
        db.close()
        return completions
    }

    fun completeHabit(habitId: Long): Boolean {
        val habit = getHabitById(habitId)
        habit?.let {
            val now = System.currentTimeMillis()
            val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
            
            val lastCompleted = it.lastCompleted
            if (lastCompleted != null) {
                val calendarLast = Calendar.getInstance().apply { timeInMillis = lastCompleted }
                if (isSameDay(calendarNow, calendarLast)) {
                    return false
                }
            }

            addHabitCompletion(habitId, now)

            val shouldIncrementStreak = if (lastCompleted != null) {
                val calendarLast = Calendar.getInstance().apply { timeInMillis = lastCompleted }
                isYesterday(calendarLast, calendarNow)
            } else {
                true
            }

            val newStreak = if (shouldIncrementStreak) it.streakCount + 1 else 1
            return updateHabitStreak(habitId, newStreak, now)
        }
        return false
    }

    fun undoCompleteHabit(habitId: Long): Boolean {
        val habit = getHabitById(habitId) ?: return false
        val lastCompleted = habit.lastCompleted ?: return false

        val now = Calendar.getInstance()
        val lastCal = Calendar.getInstance().apply { timeInMillis = lastCompleted }
        if (!isSameDay(now, lastCal)) {
            return false
        }

        val db = writableDatabase

        db.delete(
            TABLE_HABIT_COMPLETIONS,
            "$COLUMN_HABIT_ID_FK = ? AND $COLUMN_COMPLETION_DATE = ?",
            arrayOf(habitId.toString(), lastCompleted.toString())
        )

        val cursor = db.query(
            TABLE_HABIT_COMPLETIONS,
            arrayOf(COLUMN_COMPLETION_DATE),
            "$COLUMN_HABIT_ID_FK = ?",
            arrayOf(habitId.toString()),
            null, null,
            "$COLUMN_COMPLETION_DATE DESC",
            "1"
        )

        var previousCompletionDate: Long? = null
        if (cursor.moveToFirst()) {
            previousCompletionDate = cursor.getLong(0)
        }
        cursor.close()

        val newStreak = maxOf(0, habit.streakCount - 1)

        val values = ContentValues().apply {
            put(COLUMN_STREAK_COUNT, newStreak)
            put(COLUMN_LAST_COMPLETED, previousCompletionDate)
        }

        val result = db.update(TABLE_HABITS, values, "$COLUMN_ID = ?", arrayOf(habitId.toString()))
        db.close()

        return result > 0
    }

    private fun isYesterday(last: Calendar, now: Calendar): Boolean {
        val yesterday = now.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(last, yesterday)
    }

    fun resetHabitStreak(habitId: Long): Boolean {
        return updateHabitStreak(habitId, 0, null)
    }

    fun deleteHabit(habitId:Long): Boolean{
        val db = writableDatabase
        db.delete(TABLE_HABIT_COMPLETIONS, "$COLUMN_HABIT_ID_FK = ?", arrayOf(habitId.toString()))
        val rowsDeleted = db.delete(TABLE_HABITS, "$COLUMN_ID = ?", arrayOf(habitId.toString()))
        db.close()
        return rowsDeleted > 0
    }

    public fun getHabitById(habitId: Long): Habit? {
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

    fun getHabitStatistics(habitId: Long): HabitStatistics {
        val completions = getHabitCompletions(habitId)
        val habit = getHabitById(habitId) ?: return HabitStatistics(0, 0, 0, 0, 0, emptyList())

        val totalDays = calculateTotalDays(habit.createdAt)
        val completedDays = completions.size
        val completionRate = if (totalDays > 0) (completedDays.toDouble() / totalDays * 100).toInt() else 0

        val currentStreak = habit.streakCount
        val recordStreak = calculateRecordStreak(completions)

        return HabitStatistics(
            totalDays = totalDays,
            completedDays = completedDays,
            completionRate = completionRate,
            currentStreak = currentStreak,
            recordStreak = recordStreak,
            completions = completions
        )
    }

    private fun calculateTotalDays(createdAt: Long): Int {
        val createdDate = Calendar.getInstance().apply { timeInMillis = createdAt }
        val currentDate = Calendar.getInstance()
        val diffInMillis = currentDate.timeInMillis - createdDate.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt() + 1
    }

    private fun calculateRecordStreak(completions: List<Long>): Int {
        if (completions.isEmpty()) return 0
        var maxStreak = 1
        var currentStreak = 1
        for (i in 1 until completions.size) {
            val prevDate = Calendar.getInstance().apply { timeInMillis = completions[i-1] }
            val currDate = Calendar.getInstance().apply { timeInMillis = completions[i] }
            if (isConsecutiveDay(prevDate, currDate)) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        return maxStreak
    }

    private fun isConsecutiveDay(day1: Calendar, day2: Calendar): Boolean {
        val diff = day2.timeInMillis - day1.timeInMillis
        return diff <= (24 * 60 * 60 * 1000L)
    }

    public fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isDataBaseEmpty(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_HABITS", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()){
                count = it.getInt(0)
            }
        }
        db.close()
        return count == 0
    }

    fun countHabits(): Int{
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_HABITS", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()){
                count = it.getInt(0)
            }
        }
        db.close()
        return count
    }
}