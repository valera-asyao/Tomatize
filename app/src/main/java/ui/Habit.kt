package ui

data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String,
    val type: HabitType,
    val streakCount: Int = 0,
    val lastCompleted: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class HabitType {
    GOOD, BAD
}