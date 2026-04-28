package ui

import android.content.Context
import com.example.tomatize.UserData

data class TomatoHealthState(
    val hearts: Int,
    val nextHeartAt: Long?,
    val deathCount: Int
) {
    val isDead: Boolean
        get() = hearts <= 0
}

data class TomatoHealthChange(
    val state: TomatoHealthState,
    val died: Boolean
)

object TomatoHealthStorage {

    const val MAX_HEARTS = 3
    const val HEART_RESTORE_DAYS = 3

    private const val KEY_HEARTS = "TOMATO_HEARTS"
    private const val KEY_HEART_CLOCK = "TOMATO_HEART_CLOCK"
    private const val KEY_DEATH_COUNT = "TOMATO_DEATH_COUNT"
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val HEART_RESTORE_MS = HEART_RESTORE_DAYS * DAY_MS

    private fun prefs(context: Context) =
        context.getSharedPreferences(UserData.PREFS_NAME, Context.MODE_PRIVATE)

    fun getState(context: Context, now: Long = System.currentTimeMillis()): TomatoHealthState {
        restoreHearts(context, now)

        val hearts = readHearts(context)
        val clock = prefs(context).getLong(KEY_HEART_CLOCK, 0L)
        val nextHeartAt = if (hearts in 0 until MAX_HEARTS && clock > 0L) {
            clock + HEART_RESTORE_MS
        } else {
            null
        }

        return TomatoHealthState(
            hearts = hearts,
            nextHeartAt = nextHeartAt,
            deathCount = getDeathCount(context)
        )
    }

    fun loseHearts(
        context: Context,
        amount: Int,
        now: Long = System.currentTimeMillis()
    ): TomatoHealthChange {
        restoreHearts(context, now)

        val damage = amount.coerceAtLeast(0)
        if (damage == 0) {
            return TomatoHealthChange(getState(context, now), died = false)
        }

        val currentHearts = readHearts(context)
        val newHearts = (currentHearts - damage).coerceAtLeast(0)
        val editor = prefs(context).edit()
            .putInt(KEY_HEARTS, newHearts)

        if (currentHearts == MAX_HEARTS && newHearts < MAX_HEARTS) {
            editor.putLong(KEY_HEART_CLOCK, now)
        }

        editor.apply()

        val died = newHearts == 0
        if (died) {
            applyDeathPenalty(context)
        }

        return TomatoHealthChange(getState(context, now), died)
    }

    fun restoreHearts(context: Context, now: Long = System.currentTimeMillis()) {
        val currentHearts = readHearts(context)
        val savedClock = prefs(context).getLong(KEY_HEART_CLOCK, 0L)

        if (currentHearts >= MAX_HEARTS) {
            prefs(context).edit().remove(KEY_HEART_CLOCK).apply()
            return
        }

        if (savedClock == 0L) {
            prefs(context).edit().putLong(KEY_HEART_CLOCK, now).apply()
            return
        }

        val restoredCount = ((now - savedClock) / HEART_RESTORE_MS).toInt()
        if (restoredCount <= 0) return

        val newHearts = (currentHearts + restoredCount).coerceAtMost(MAX_HEARTS)
        val editor = prefs(context).edit()
            .putInt(KEY_HEARTS, newHearts)

        if (newHearts >= MAX_HEARTS) {
            editor.remove(KEY_HEART_CLOCK)
        } else {
            editor.putLong(KEY_HEART_CLOCK, savedClock + restoredCount * HEART_RESTORE_MS)
        }

        editor.apply()
    }

    fun resetHearts(context: Context) {
        prefs(context).edit()
            .putInt(KEY_HEARTS, MAX_HEARTS)
            .remove(KEY_HEART_CLOCK)
            .apply()
    }

    fun damageForHabit(habit: Habit): Int = habit.heartDamage

    fun getDeathCount(context: Context): Int =
        prefs(context).getInt(KEY_DEATH_COUNT, 0)

    fun applyDeathPenalty(context: Context) {
        val newDeathCount = getDeathCount(context) + 1

        ShopStorage.clearMoneyAndSkins(context)

        prefs(context).edit()
            .putInt(KEY_DEATH_COUNT, newDeathCount)
            .putInt(KEY_HEARTS, MAX_HEARTS)
            .remove(KEY_HEART_CLOCK)
            .apply()
    }

    private fun readHearts(context: Context): Int =
        prefs(context).getInt(KEY_HEARTS, MAX_HEARTS).coerceIn(0, MAX_HEARTS)
}
