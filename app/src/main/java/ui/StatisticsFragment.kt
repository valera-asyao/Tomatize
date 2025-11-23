package ui

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tomatize.R
import com.google.android.material.button.MaterialButton

class StatisticsFragment : Fragment() {
    private lateinit var databaseHelper: HabitDatabaseHelper
    private lateinit var mainContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainContainer = view.findViewById(R.id.mainContainer)
        databaseHelper = HabitDatabaseHelper(requireContext())
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

        val headerText = TextView(requireContext()).apply {
            text = "Статистика"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
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
        val habitButton = MaterialButton(requireContext()).apply {
            val x = habit.streakCount
            if (x % 10 == 0 || x % 10 == 5 || x % 10 == 6 || x % 10 == 7 || x % 10 == 8 || x % 10 == 9 || (x in 10..20))
                text = "${habit.name}\n🔥 ${x} дней"
            else if (x % 10 == 1)
                text = "${habit.name}\n🔥 ${x} день"
            else
                text = "${habit.name}\n🔥 ${x} дня"
            setOnClickListener {
                onHabitClicked(habit)
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }

            cornerRadius = 32;
            textSize = 16f
            isAllCaps = false
            setPadding(32, 32, 32, 32)
            elevation = 0f

            when (habit.type) {
                HabitType.GOOD -> {
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.good_habit_color))
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                HabitType.BAD -> {
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.bad_habit_color))
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
            insetTop = 0
            insetBottom = 0
            strokeWidth = 0
        }
        container.addView(habitButton)
    }

    private fun onHabitClicked(habit: Habit) {
        navigateToHabitStatistics(habit)
    }

    private fun navigateToHabitStatistics(habit: Habit){
    }
}