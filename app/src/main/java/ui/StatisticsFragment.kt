package ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tomatize.R
import com.google.android.material.button.MaterialButton
import android.widget.Toast

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
            setTextColor(getThemeColor(android.R.attr.textColorSecondary))

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
            setTextColor(getThemeColor(android.R.attr.textColorPrimary))
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
                openHabitStatistics(habit)
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

    private fun openHabitStatistics(habit: Habit) {
        try {
            val habitStatisticsFragment = HabitStatisticsFragment.newInstance(habit.id)

            // Проверяем, что контейнер существует
            val containerId = R.id.nav_host_fragment
            if (requireActivity().findViewById<View>(containerId) == null) {
                throw IllegalStateException("Контейнер фрагментов не найден. ID: $containerId")
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(containerId, habitStatisticsFragment)
                .addToBackStack("habit_statistics")
                .commit()

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