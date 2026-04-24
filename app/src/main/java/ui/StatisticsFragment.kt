package ui

import android.content.res.ColorStateList
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
        val habits = databaseHelper.getAllHabits()
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
        mainContainer.addView(habitsContainer)
    }

    private fun addHabitButton(container: LinearLayout, habit: Habit) {
        val habitView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_habit_statistics, container, false)

        val nameTextView = habitView.findViewById<TextView>(R.id.habitNameTextView)
        val mainItemContainer = habitView.findViewById<View>(R.id.habitItemContainer)

        nameTextView.text = habit.name

        val context = requireContext()
        
        // Правильное определение цветов через R.color
        val (bgColor, txtColor) = if (habit.type == HabitType.GOOD) {
            Pair(
                ContextCompat.getColor(context, R.color.habit_good_bg),
                ContextCompat.getColor(context, R.color.habit_good_text)
            )
        } else {
            Pair(
                ContextCompat.getColor(context, R.color.habit_bad_bg),
                ContextCompat.getColor(context, R.color.habit_bad_text)
            )
        }

        mainItemContainer.backgroundTintList = ColorStateList.valueOf(bgColor)
        nameTextView.setTextColor(txtColor)

        mainItemContainer.setOnClickListener {
            openHabitStatistics(habit)
        }

        container.addView(habitView)
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
