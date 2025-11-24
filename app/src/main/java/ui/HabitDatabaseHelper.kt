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
            // Создаем таблицу выполнений при обновлении базы
            val createCompletionsTable = """
            CREATE TABLE $TABLE_HABIT_COMPLETIONS (
                $COLUMN_COMPLETION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_HABIT_ID_FK INTEGER NOT NULL,
                $COLUMN_COMPLETION_DATE INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_HABIT_ID_FK) REFERENCES $TABLE_HABITS($COLUMN_ID)
            )
        """.trimIndent()
            db.execSQL(createCompletionsTable)

            // Миграция существующих данных
            migrateExistingCompletions(db)
        }
    }

    private fun migrateExistingCompletions(db: SQLiteDatabase) {
        // Переносим существующие last_completed в новую таблицу
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

    // Метод для добавления выполнения привычки
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

    // Получение всех выполнений привычки
    fun getHabitCompletions(habitId: Long): List<Long> {
        val completions = mutableListOf<Long>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HABIT_COMPLETIONS,
            arrayOf(COLUMN_COMPLETION_DATE),
            "$COLUMN_HABIT_ID_FK = ?",
            arrayOf(habitId.toString()),
            null, null,
            "$COLUMN_COMPLETION_DATE ASC" // Сортируем по возрастанию даты
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

    // Получение выполнений за определенный период
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

            // Добавляем запись в историю
            addHabitCompletion(habitId, now)

            // Обновляем streak (существующая логика)
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
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
    // Расчет статистики за период
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
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt() + 1 // +1 чтобы включить день создания
    }

    private fun calculateCurrentStreak(completions: List<Long>): Int {
        if (completions.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        var streak = 0
        var currentDate = Calendar.getInstance()

        // Сортировка по убыванию даты
        val sortedCompletions = completions.sortedDescending()

        for (completion in sortedCompletions) {
            currentDate.timeInMillis = completion
            if (isConsecutiveDay(calendar, currentDate)) {
                streak++
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }

        return streak
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

    // Проверка БД на не пустоту
    fun isDataBaseEmpty(): Boolean {
        val db = readableDatabase;
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_HABITS", null);
        var count = 0;
        cursor.use {
            if (it.moveToFirst()){
                count = it.getInt(0);
            }
        }
        db.close();
        return count == 0;
    }

    // Подсчёт количества привычек в БД
    fun countHabits(): Int{
        val db = readableDatabase;
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_HABITS", null);
        var count = 0;
        cursor.use {
            if (it.moveToFirst()){
                count = it.getInt(0);
            }
        }
        db.close();
        return count;
    }
    // Получение истории выполнений привычки (для календаря)
//    fun getHabitCompletionHistory(habitId: Long): List<Long> {
//        val completions = mutableListOf<Long>()
//        val habit = getHabitById(habitId)
//
//        habit?.lastCompleted?.let {
//            completions.add(it)
//        }
//
//        return completions
//    }

    // Проверка выполнения привычки в конкретную дату
//    fun isHabitCompletedOnDate(habitId: Long, dateInMillis: Long): Boolean {
//        val habit = getHabitById(habitId) ?: return false
//        if (habit.lastCompleted == null) return false
//
//        val habitDate = Calendar.getInstance().apply { timeInMillis = habit.lastCompleted!! }
//        val targetDate = Calendar.getInstance().apply { timeInMillis = dateInMillis }
//
//        return isSameDay(habitDate, targetDate)
//    }
//
}