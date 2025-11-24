package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tomatize.R
import java.util.Calendar

class HabitStatisticsFragment : Fragment() {

    private lateinit var databaseHelper: HabitDatabaseHelper
    private var habitId: Long = -1

    companion object {
        private const val ARG_HABIT_ID = "habit_id"

        fun newInstance(habitId: Long): HabitStatisticsFragment {
            val fragment = HabitStatisticsFragment()
            val args = Bundle()
            args.putLong(ARG_HABIT_ID, habitId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseHelper = HabitDatabaseHelper(requireContext())

        arguments?.let {
            habitId = it.getLong(ARG_HABIT_ID, -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackButton()
        loadHabitStatistics()
    }

    private fun setupBackButton() {
        view?.findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun loadHabitStatistics() {
        if (habitId == -1L) return

        val habit = databaseHelper.getHabitById(habitId)
        val statistics = databaseHelper.getHabitStatistics(habitId)

        habit?.let {
            displayHabitData(it, statistics)
            setupCurrentWeekCalendar(it)
            setupMonthlyCalendar(statistics.completions)
        }
    }

    private fun showError(message: String) {
        // Можно показать Toast или изменить интерфейс для отображения ошибки
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun displayHabitData(habit: Habit, statistics: HabitStatistics) {
        // Заголовок
        view?.findViewById<TextView>(R.id.header)?.text = habit.name.uppercase()

        // Общая статистика
        view?.findViewById<TextView>(R.id.habitDescription)?.text =
            "Выполнено: ${statistics.completedDays} из ${statistics.totalDays} дней"

        view?.findViewById<TextView>(R.id.habitDescription)?.text =
            habit.description ?: "Описание отсутствует"

        // Серии
        val seriesText = when {
            statistics.currentStreak % 10 == 1 && statistics.currentStreak % 100 != 11 ->
                "СЕРИЯ ИЗ\n${statistics.currentStreak} ДНЯ"
            statistics.currentStreak % 10 in 2..4 && statistics.currentStreak % 100 !in 12..14 ->
                "СЕРИЯ ИЗ\n${statistics.currentStreak} ДНЕЙ"
            else -> "СЕРИЯ ИЗ\n${statistics.currentStreak} ДНЕЙ"
        }

        val recordText = when {
            statistics.recordStreak % 10 == 1 && statistics.recordStreak % 100 != 11 ->
                "РЕКОРД:\n${statistics.recordStreak} ДЕНЬ"
            statistics.recordStreak % 10 in 2..4 && statistics.recordStreak % 100 !in 12..14 ->
                "РЕКОРД:\n${statistics.recordStreak} ДНЯ"
            else -> "РЕКОРД:\n${statistics.recordStreak} ДНЕЙ"
        }

        view?.findViewById<TextView>(R.id.series)?.text = seriesText
        view?.findViewById<TextView>(R.id.record)?.text = recordText
    }

    private fun calculateHabitExperience(createdAt: Long): String {
        val createdDate = Calendar.getInstance().apply { timeInMillis = createdAt }
        val currentDate = Calendar.getInstance()

        val diffInMillis = currentDate.timeInMillis - createdDate.timeInMillis
        val days = (diffInMillis / (24 * 60 * 60 * 1000)).toInt()

        return when {
            days < 30 -> "$days дней"
            days < 365 -> "${days / 30} месяцев"
            else -> "${days / 365} лет"
        }
    }

    private fun setupCurrentWeekCalendar(habit: Habit) {
        try {
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
            }

            // Устанавливаем на понедельник текущей недели
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            val dayIds = listOf(
                R.id.currentDay1, R.id.currentDay2, R.id.currentDay3, R.id.currentDay4,
                R.id.currentDay5, R.id.currentDay6, R.id.currentDay7
            )

            val today = Calendar.getInstance()

            for (i in 0 until 7) {
                val dayView = view?.findViewById<TextView>(dayIds[i])
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)



                // Проверяем, выполнен ли день
                val isCompleted = isHabitCompletedOnDate(habit, calendar.timeInMillis)
                val isToday = isSameDay(calendar, today)

                when {
                    isCompleted -> {
                        dayView?.setBackgroundResource(R.drawable.day_circle_completed)
                        dayView?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    isToday -> {
                        dayView?.text = dayOfMonth.toString()
                        dayView?.setBackgroundResource(R.drawable.day_circle_today)
                        dayView?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    }
                    else -> {
                        dayView?.text = dayOfMonth.toString()
                        dayView?.setBackgroundResource(R.drawable.day_circle)
                        dayView?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    }
                }

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupMonthlyCalendar(completions: List<Long>) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Устанавливаем на первое число текущего месяца
        calendar.set(currentYear, currentMonth, 1)

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dayIds = getMonthlyCalendarDayIds()

        // Сбрасываем все дни
        dayIds.forEach { id ->
            view?.findViewById<TextView>(id)?.apply {
                text = ""
                setBackgroundResource(0)
                visibility = View.INVISIBLE
            }
        }

        // Заполняем календарь с учетом всей истории выполнений
        var dayCounter = 1
        val today = Calendar.getInstance()

        val startPosition = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        for (position in 0 until 42) {
            if (position >= startPosition && dayCounter <= daysInMonth) {
                val dayView = view?.findViewById<TextView>(dayIds[position])
                dayView?.apply {

                    visibility = View.VISIBLE

                    // Проверяем выполнение для этого дня
                    calendar.set(currentYear, currentMonth, dayCounter)
                    val isCompleted = isDateInCompletions(calendar.timeInMillis, completions)
                    val isToday = isSameDay(calendar, today)

                    when {
                        isCompleted -> {
                            setBackgroundResource(R.drawable.day_circle_completed)
                            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                        }
                        isToday -> {
                            text = dayCounter.toString()
                            setBackgroundResource(R.drawable.day_circle_today)
                            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                        }
                        else -> {
                            text = dayCounter.toString()
                            setBackgroundResource(R.drawable.day_circle)
                            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                        }
                    }
                }
                dayCounter++
            }
        }
    }

    private fun isDateInCompletions(dateInMillis: Long, completions: List<Long>): Boolean {
        val targetDate = Calendar.getInstance().apply { timeInMillis = dateInMillis }
        return completions.any { completion ->
            val completionDate = Calendar.getInstance().apply { timeInMillis = completion }
            isSameDay(targetDate, completionDate)
        }
    }

    private fun isHabitCompletedOnDate(habit: Habit, dateInMillis: Long): Boolean {
        // Упрощенная проверка - сравниваем только с lastCompleted
        if (habit.lastCompleted == null) return false

        val habitDate = Calendar.getInstance().apply { timeInMillis = habit.lastCompleted!! }
        val targetDate = Calendar.getInstance().apply { timeInMillis = dateInMillis }

        return isSameDay(habitDate, targetDate)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun getMonthlyCalendarDayIds(): List<Int> {
        return listOf(
            R.id.calDay1, R.id.calDay2, R.id.calDay3, R.id.calDay4, R.id.calDay5, R.id.calDay6, R.id.calDay7,
            R.id.calDay8, R.id.calDay9, R.id.calDay10, R.id.calDay11, R.id.calDay12, R.id.calDay13, R.id.calDay14,
            R.id.calDay15, R.id.calDay16, R.id.calDay17, R.id.calDay18, R.id.calDay19, R.id.calDay20, R.id.calDay21,
            R.id.calDay22, R.id.calDay23, R.id.calDay24, R.id.calDay25, R.id.calDay26, R.id.calDay27, R.id.calDay28,
            R.id.calDay29, R.id.calDay30, R.id.calDay31, R.id.calDay32, R.id.calDay33, R.id.calDay34, R.id.calDay35,
            R.id.calDay36, R.id.calDay37, R.id.calDay38, R.id.calDay39, R.id.calDay40, R.id.calDay41, R.id.calDay42
        )
    }
}