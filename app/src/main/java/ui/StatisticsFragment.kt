package ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tomatize.R

class StatisticsFragment : Fragment() {
    private lateinit var databaseHelper: HabitDatabaseHelper
    private lateinit var mainContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainContainer = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32);

        }
        return mainContainer
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseHelper = HabitDatabaseHelper(requireContext())
        checkAndDisplayHabits()
        checkAndDisplayHabits()
    }

    override fun onResume() {
        super.onResume()
        checkAndDisplayHabits()
    }

    private fun checkAndDisplayHabits() {
        mainContainer.removeAllViews()

        if (databaseHelper.isDataBaseEmpty()) {
            showEmptyState()
        } else {
            showHabitsList()
        }
    }

    private fun showEmptyState() {
        val emptyText = TextView(requireContext()).apply {
            text = "Нет добавленных привычек"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        mainContainer.addView(emptyText)
    }

    private fun showHabitsList() {
        val habits = databaseHelper.getAllHabits()

        // Заголовок
        val headerText = TextView(requireContext()).apply {
            text = "Статистика привычек (${habits.size})"
            textSize = 18f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            setPadding(0, 0, 0, 32)
        }
        mainContainer.addView(headerText)

        val habitsContainer = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        habits.forEach { habit ->
            addHabitButton(habitsContainer, habit)
        }

        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        scrollView.addView(habitsContainer)
        mainContainer.addView(scrollView)
    }

    private fun addHabitButton(container: LinearLayout, habit: Habit) {
        val habitButton = Button(requireContext()).apply {
            text = "${habit.name}\n🔥 ${habit.streakCount} дней"
            setOnClickListener {
                onHabitClicked(habit)
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }

            textSize = 16f
            isAllCaps = false
            setPadding(32, 32, 32, 32)

            when (habit.type) {
                HabitType.GOOD -> {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.good_habit_color))
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                HabitType.BAD -> {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.bad_habit_color))
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
        }
        container.addView(habitButton)
    }

    private fun onHabitClicked(habit: Habit) {
        val success = databaseHelper.completeHabit(habit.id)

        if (success) {
            checkAndDisplayHabits()
            Toast.makeText(
                requireContext(),
                "Привычка '${habit.name}' выполнена!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}