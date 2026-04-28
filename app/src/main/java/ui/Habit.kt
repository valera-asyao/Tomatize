package ui

const val MIN_BAD_DIFFICULTY = 1
const val MAX_BAD_DIFFICULTY = 3
const val DEFAULT_BAD_DIFFICULTY = 1

data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String,
    val type: HabitType,
    val streakCount: Int = 0,
    val lastCompleted: Long? = null,
    val badDifficulty: Int = DEFAULT_BAD_DIFFICULTY,
    val createdAt: Long = System.currentTimeMillis()
) {
    val heartDamage: Int
        get() = if (type == HabitType.BAD) normalizeBadDifficulty(badDifficulty) else 0
}

enum class HabitType {
    GOOD, BAD
}

fun normalizeBadDifficulty(value: Int): Int =
    value.coerceIn(MIN_BAD_DIFFICULTY, MAX_BAD_DIFFICULTY)
