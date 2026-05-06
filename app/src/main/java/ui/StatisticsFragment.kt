package ui

import android.graphics.drawable.GradientDrawable
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
import androidx.navigation.navOptions

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

    fun refreshHabits() {
        if (isAdded) {
            checkAndDisplayHabits()
        }
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
                topMargin = 100
            }
        }
        mainContainer.addView(emptyText)
    }

    private fun showHabitsList() {
        val habits = databaseHelper.getAllHabits().sortedBy { if (it.type == HabitType.BAD) 0 else 1 }
        val habitsContainer = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        habits.forEach { habit ->
            addHabitBlock(habitsContainer, habit)
        }
        mainContainer.addView(habitsContainer)
    }

    private fun addHabitBlock(container: LinearLayout, habit: Habit) {
        val habitView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_habit_statistics, container, false)

        val nameTextView = habitView.findViewById<TextView>(R.id.habitNameTextView)
        val streakTextView = habitView.findViewById<TextView>(R.id.streakTextView)
        val mainItemContainer = habitView.findViewById<View>(R.id.habitItemContainer)
        val indicatorSection1 = habitView.findViewById<View>(R.id.indicatorSection1)
        val indicatorSection2 = habitView.findViewById<View>(R.id.indicatorSection2)
        val indicatorSection3 = habitView.findViewById<View>(R.id.indicatorSection3)

        nameTextView.text = habit.name
        streakTextView.text = "Стрик ${habit.streakCount} дн."

        val context = requireContext()
        val indicatorColor = if (habit.type == HabitType.GOOD) {
            ContextCompat.getColor(context, R.color.good_habit_color)
        } else {
            ContextCompat.getColor(context, R.color.bad_habit_color)
        }

        val visibleCount = if (habit.type == HabitType.BAD) {
            normalizeBadDifficulty(habit.badDifficulty)
        } else {
            1
        }

        val sections = listOf(indicatorSection1, indicatorSection2, indicatorSection3)
        sections.forEachIndexed { index, section ->
            section.visibility = if (index < visibleCount) View.VISIBLE else View.GONE
            if (index < visibleCount) {
                val topRounded = when (visibleCount) {
                    1 -> true
                    2 -> index == 0
                    3 -> index == 0
                    else -> false
                }
                val bottomRounded = when (visibleCount) {
                    1 -> true
                    2 -> index == 1
                    3 -> index == 2
                    else -> false
                }
                section.background = createIndicatorDrawable(indicatorColor, topRounded, bottomRounded)
            }
        }

        mainItemContainer.setOnClickListener {
            openHabitStatistics(habit)
        }

        container.addView(habitView)
    }

    private fun createIndicatorDrawable(color: Int, topRounded: Boolean, bottomRounded: Boolean): GradientDrawable {
        val radius = dpToPx(8).toFloat()
        val radii = floatArrayOf(
            if (topRounded) radius else 0f,
            if (topRounded) radius else 0f,
            if (topRounded) radius else 0f,
            if (topRounded) radius else 0f,
            if (bottomRounded) radius else 0f,
            if (bottomRounded) radius else 0f,
            if (bottomRounded) radius else 0f,
            if (bottomRounded) radius else 0f
        )
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadii = radii
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun openHabitStatistics(habit: Habit) {
        try {
            val bundle = Bundle().apply {
                putLong("habit_id", habit.id)
            }
            
            val options = navOptions {
                anim {
                    enter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popEnter = R.anim.slide_in_left
                    popExit = R.anim.slide_out_right
                }
            }
            
            findNavController().navigate(R.id.habitStatisticsFragment, bundle, options)
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Ошибка открытия статистики")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
