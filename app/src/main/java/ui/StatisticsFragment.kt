package ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tomatize.R
import android.widget.Toast
import androidx.navigation.fragment.findNavController

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
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(getThemeColor(android.R.attr.textColorSecondary))
            try {
                typeface = resources.getFont(R.font.gnf)
            } catch (e: Exception) {}

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 100, 0, 0)
            }
        }
        mainContainer.addView(emptyText)
    }

    private fun showHabitsList() {
        val habits = databaseHelper.getAllHabits()

        // Header is now in fragment_statistics.xml to match Shop fragment

        habits.forEach { habit ->
            addHabitButton(mainContainer, habit)
        }
    }

    private fun addHabitButton(container: LinearLayout, habit: Habit) {
        val habitView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_habit_statistics, container, false)


        val nameTextView = habitView.findViewById<TextView>(R.id.habitNameTextView)
        val typeTextView = habitView.findViewById<TextView>(R.id.typeOfHabit)
        val indicatorView = habitView.findViewById<View>(R.id.typeIndicatorView)
        val mainItemContainer = habitView.findViewById<View>(R.id.habitItemContainer)

        val x = habit.streakCount
        val daysText = when {
            x % 100 in 11..19 -> "$x дней"
            x % 10 == 1 -> "$x день"
            x % 10 in 2..4 -> "$x дня"
            else -> "$x дней"
        }

        nameTextView.text = habit.name
        typeTextView.text = daysText

        val colorRes = if (habit.type == HabitType.GOOD) {
            R.color.good_habit_color
        } else {
            R.color.bad_habit_color
        }
        val color = ContextCompat.getColor(requireContext(), colorRes)

        indicatorView.setBackgroundColor(color)

        val clickableArea = mainItemContainer ?: habitView
        clickableArea.setOnClickListener {
            openHabitStatistics(habit)
        }

        container.addView(habitView)
    }

    private fun openHabitStatistics(habit: Habit) {
        try {
            val bundle = Bundle().apply {
                putLong("habit_id", habit.id)
            }
            findNavController().navigate(R.id.habitStatisticsFragment, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Ошибка открытия статистики: ${e.message}")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun getThemeColor(attrRes: Int): Int {
        return try {
            val typedArray = requireContext().theme.obtainStyledAttributes(intArrayOf(attrRes))
            val color = typedArray.getColor(0, ContextCompat.getColor(requireContext(), android.R.color.black))
            typedArray.recycle()
            color
        } catch (e: Exception) {
            ContextCompat.getColor(requireContext(), android.R.color.black)
        }
    }
}