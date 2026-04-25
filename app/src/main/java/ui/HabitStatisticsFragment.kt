package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tomatize.MainActivity
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
        (activity as? MainActivity)?.setNavBarVisibility(false)
        (activity as? MainActivity)?.disableNavBarSelection()
        setupDeleteButton()
        setupBackButton()
        loadHabitStatistics()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.setNavBarVisibility(true)
        (activity as? MainActivity)?.enableNavBarSelection()
    }

    private fun setupBackButton() {
        view?.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun showDeleteConfirmation() {
        activity?.let { context ->
            android.app.AlertDialog.Builder(context)
                .setTitle("Удалить привычку?")
                .setMessage("Вы уверены, что хотите удалить эту привычку? Все данные будут безвозвратно удалены.")
                .setPositiveButton("Удалить") { _, _ ->
                    val success = databaseHelper.deleteHabit(habitId)
                    if (success) {
                        android.widget.Toast.makeText(context, "Привычка удалена", android.widget.Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        android.widget.Toast.makeText(context, "Ошибка при удалении", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun setupDeleteButton() {
        view?.findViewById<ImageButton>(R.id.deleteButton)?.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadHabitStatistics() {
        if (habitId == -1L) return

        val habit = databaseHelper.getHabitById(habitId)
        val statistics = databaseHelper.getHabitStatistics(habitId)

        habit?.let {
            displayHabitData(it, statistics)
            setupCurrentWeekCalendar(it, statistics.completions)
            setupMonthlyCalendar(it, statistics.completions)
        }
    }

    private fun displayHabitData(habit: Habit, statistics: HabitStatistics) {
        view?.findViewById<TextView>(R.id.header)?.text = habit.name.uppercase()
        view?.findViewById<TextView>(R.id.habitDescription)?.text =
            if (habit.description.isNullOrBlank()) "Описания привычки нет" else habit.description

        val seriesText = "СЕРИЯ ИЗ\n${statistics.currentStreak} ${getDayWord(statistics.currentStreak)}"
        val recordText = "РЕКОРД:\n${statistics.recordStreak} ${getDayWord(statistics.recordStreak)}"

        view?.findViewById<TextView>(R.id.series)?.text = seriesText
        view?.findViewById<TextView>(R.id.record)?.text = recordText
    }

    private fun getDayWord(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "ДЕНЬ"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "ДНЯ"
            else -> "ДНЕЙ"
        }
    }

    private fun setupCurrentWeekCalendar(habit: Habit, completions: List<Long>) {
        try {
            val calendar = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }

            val dayIds = listOf(
                R.id.currentDay1, R.id.currentDay2, R.id.currentDay3, R.id.currentDay4,
                R.id.currentDay5, R.id.currentDay6, R.id.currentDay7
            )

            val today = Calendar.getInstance()
            val createdAtCal = Calendar.getInstance().apply { timeInMillis = habit.createdAt }

            for (i in 0 until 7) {
                val dayView = view?.findViewById<TextView>(dayIds[i])
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                
                dayView?.text = dayOfMonth.toString()

                val isMarkedInDb = isDateInCompletions(calendar.timeInMillis, completions)
                val isToday = isSameDay(calendar, today)
                val isBeforeToday = calendar.before(today) && !isToday
                val existedAtThatDay = isSameDay(calendar, createdAtCal) || calendar.after(createdAtCal)

                updateDayStyle(dayView, isMarkedInDb, isToday, isBeforeToday, habit.type, existedAtThatDay)

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupMonthlyCalendar(habit: Habit, completions: List<Long>) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.set(currentYear, currentMonth, 1)

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dayIds = getMonthlyCalendarDayIds()

        dayIds.forEach { id ->
            view?.findViewById<TextView>(id)?.apply {
                text = ""
                setBackgroundResource(0)
                visibility = View.INVISIBLE
            }
        }

        var dayCounter = 1
        val today = Calendar.getInstance()
        val createdAtCal = Calendar.getInstance().apply { timeInMillis = habit.createdAt }

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
                    text = dayCounter.toString()

                    calendar.set(currentYear, currentMonth, dayCounter)
                    val isMarkedInDb = isDateInCompletions(calendar.timeInMillis, completions)
                    val isToday = isSameDay(calendar, today)
                    val isBeforeToday = calendar.before(today) && !isToday
                    val existedAtThatDay = isSameDay(calendar, createdAtCal) || calendar.after(createdAtCal)

                    updateDayStyle(this, isMarkedInDb, isToday, isBeforeToday, habit.type, existedAtThatDay)
                }
                dayCounter++
            }
        }
    }

    private fun updateDayStyle(dayView: TextView?, isMarkedInDb: Boolean, isToday: Boolean, isBeforeToday: Boolean, habitType: HabitType, existedAtThatDay: Boolean) {
        dayView?.apply {
            if (!existedAtThatDay) {
                setBackgroundResource(0)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                alpha = 0.3f // Делаем дни до создания полупрозрачными
                return
            }
            alpha = 1.0f

            if (habitType == HabitType.GOOD) {
                when {
                    isMarkedInDb -> {
                        setBackgroundResource(R.drawable.day_circle_green)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    isToday -> {
                        setBackgroundResource(R.drawable.day_circle_black)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    isBeforeToday -> {
                        setBackgroundResource(R.drawable.day_circle_red)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    else -> {
                        setBackgroundResource(0)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    }
                }
            } else { // BAD Habit
                when {
                    isMarkedInDb -> { // Срыв (записан в БД)
                        setBackgroundResource(R.drawable.day_circle_red)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    isToday -> {
                        setBackgroundResource(R.drawable.day_circle_black)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    isBeforeToday -> { // Соблюдение (не было срыва)
                        setBackgroundResource(R.drawable.day_circle_green)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    else -> {
                        setBackgroundResource(0)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    }
                }
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

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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
