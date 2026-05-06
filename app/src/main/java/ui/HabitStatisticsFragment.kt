package ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tomatize.MainActivity
import com.example.tomatize.R
import com.example.tomatize.UserData
import com.google.android.material.snackbar.Snackbar
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
        setupEditButton()
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_habit, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancelDelete).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnConfirmDelete).setOnClickListener {
            if (databaseHelper.deleteHabit(habitId)) {
                (activity as? MainActivity)?.showTopNotification("Привычка удалена")
                dialog.dismiss()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupDeleteButton() {
        view?.findViewById<ImageButton>(R.id.deleteButton)?.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun setupEditButton() {
        view?.findViewById<ImageButton>(R.id.editButton)?.setOnClickListener {
            showEditDialog()
        }
    }

    private fun showEditDialog() {
        val editDialog = EditHabitDialog.newInstance(habitId)
        editDialog.setOnHabitEditedListener {
            loadHabitStatistics()
            (activity as? MainActivity)?.showTopNotification("Привычка обновлена")
        }
        editDialog.show(parentFragmentManager, "EditHabitDialog")
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
        val recordText = "РЕКОРД:\n${statistics.recordStreak} ${getStreak(statistics.recordStreak)}"

        view?.findViewById<TextView>(R.id.series)?.text = seriesText
        view?.findViewById<TextView>(R.id.record)?.text = recordText
    }

    private fun getDayWord(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "ДНЯ"
            else -> "ДНЕЙ"
        }
    }

    private fun getStreak(count: Int): String{
        return when {
            count % 10 == 1 -> "ДЕНЬ"
            count in 2..4 -> "ДНЯ"
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
                val dayMillis = calendar.timeInMillis

                dayView?.text = dayOfMonth.toString()

                val isMarkedInDb = isDateInCompletions(dayMillis, completions)
                val isToday = isSameDay(calendar, today)
                val isBeforeToday = calendar.before(today) && !isToday
                val existedAtThatDay = isSameDay(calendar, createdAtCal) || calendar.after(createdAtCal)
                val canChange = canChangeCalendarDate(habit, dayMillis)

                dayView?.isClickable = canChange
                if (canChange) {
                    dayView?.setOnClickListener {
                        onCalendarDayClick(dayMillis)
                    }
                } else {
                    dayView?.setOnClickListener(null)
                }

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
                setOnClickListener(null)
                isClickable = false
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
                    val dayMillis = calendar.timeInMillis
                    val isMarkedInDb = isDateInCompletions(dayMillis, completions)
                    val isToday = isSameDay(calendar, today)
                    val isBeforeToday = calendar.before(today) && !isToday
                    val existedAtThatDay = isSameDay(calendar, createdAtCal) || calendar.after(createdAtCal)
                    val canChange = canChangeCalendarDate(habit, dayMillis)

                    isClickable = canChange
                    if (canChange) {
                        setOnClickListener {
                            onCalendarDayClick(dayMillis)
                        }
                    } else {
                        setOnClickListener(null)
                    }

                    updateDayStyle(this, isMarkedInDb, isToday, isBeforeToday, habit.type, existedAtThatDay)
                }
                dayCounter++
            }
        }
    }

    private fun onCalendarDayClick(dayMillis: Long) {
        val habit = databaseHelper.getHabitById(habitId) ?: return
        val wasMarked = databaseHelper.isHabitCompletedOnDate(habitId, dayMillis)
        val isToday = isToday(dayMillis)

        if (habit.type == HabitType.BAD) {
            if (wasMarked && isToday) {
                (activity as? MainActivity)?.showTopNotification("Сегодняшний срыв нельзя отменить")
                return
            }

            if (!wasMarked) {
                showBadHabitFailureBottomSheet(habit, dayMillis)
                return
            }
        }

        val changed = databaseHelper.toggleHabitCompletionOnDate(habitId, dayMillis)
        if (changed) {
            if (habit.type == HabitType.GOOD && isToday) {
                if (wasMarked) {
                    takeBackTodayReward()
                    showActionSnackbar("Выполнение отменено", true)
                } else {
                    awardCurrencyForToday()
                    showActionSnackbar("Привычка ${habit.name} соблюдена!", true)
                }
            } else if (habit.type == HabitType.BAD && wasMarked) {
                showActionSnackbar("Срыв отменен", true)
            }
            loadHabitStatistics()
        } else {
            (activity as? MainActivity)?.showTopNotification("Нельзя изменить эту дату")
        }
    }

    private fun showBadHabitFailureBottomSheet(habit: Habit, dayMillis: Long) {
        val bottomSheet = BadHabitFailureBottomSheet.newInstance(habit.id)
        bottomSheet.setOnConfirmListener {
            recordBadHabitFailure(habit, dayMillis)
        }
        bottomSheet.show(parentFragmentManager, "BadHabitFailureBottomSheet")
    }

    private fun recordBadHabitFailure(habit: Habit, dayMillis: Long) {
        val changed = if (isToday(dayMillis)) {
            databaseHelper.recordFailure(habit.id)
        } else {
            databaseHelper.toggleHabitCompletionOnDate(habit.id, dayMillis)
        }

        if (!changed) {
            (activity as? MainActivity)?.showTopNotification("Нельзя изменить эту дату")
            return
        }

        applyFailurePenalty()
        val change = TomatoHealthStorage.loseHearts(requireContext(), habit.heartDamage)
        loadHabitStatistics()

        if (change.died) {
            showTomatoDiedDialog()
        } else {
            showActionSnackbar("Срыв зафиксирован", false)
            (activity as? MainActivity)?.showTopNotification("Вы потеряли 100 \uD83C\uDF45")
        }
    }

    private fun showTomatoDiedDialog() {
        val bottomSheet = GameOverBottomSheet()
        bottomSheet.setOnConfirmListener {
            loadHabitStatistics()
        }
        bottomSheet.show(parentFragmentManager, "GameOverBottomSheet")
    }

    private fun awardCurrencyForToday() {
        val prefs = requireActivity().getSharedPreferences(UserData.PREFS_NAME, Context.MODE_PRIVATE)
        val currentDate = getTodayDateText()
        val lastRewardDate = prefs.getString("LAST_REWARD_DATE", "")
        var rewardsToday = readRewardsToday(prefs)

        if (currentDate != lastRewardDate) {
            rewardsToday = 0
            prefs.edit()
                .putString("LAST_REWARD_DATE", currentDate)
                .putInt("REWARDED_HABITS_TODAY", 0)
                .apply()
        }

        if (rewardsToday >= 3) return

        val currentBalance = prefs.getInt(UserData.KEY_CURRENCY, 0)
        val rewardAmount = 50
        val newBalance = minOf(currentBalance + rewardAmount, ShopStorage.MAX_BALANCE)
        val newRewardsToday = rewardsToday + 1

        prefs.edit()
            .putInt(UserData.KEY_CURRENCY, newBalance)
            .putInt("REWARDED_HABITS_TODAY", newRewardsToday)
            .apply()

        if (currentBalance >= ShopStorage.MAX_BALANCE) {
            (activity as? MainActivity)?.showTopNotification("Ваш баланс достиг максимума, Вы богач!")
            return
        }

        if (newBalance == ShopStorage.MAX_BALANCE && currentBalance < ShopStorage.MAX_BALANCE) {
            (activity as? MainActivity)?.showTopNotification("Ваш баланс достиг максимума, Вы богач!")
        } else {
            (activity as? MainActivity)?.showTopNotification("Награда $rewardAmount \uD83C\uDF45! ($newRewardsToday/3)")
        }
    }

    private fun takeBackTodayReward() {
        val prefs = requireActivity().getSharedPreferences(UserData.PREFS_NAME, Context.MODE_PRIVATE)
        val currentDate = getTodayDateText()
        val lastRewardDate = prefs.getString("LAST_REWARD_DATE", "")
        if (currentDate != lastRewardDate) return

        val rewardsToday = readRewardsToday(prefs)
        if (rewardsToday <= 0) return

        val currentBalance = prefs.getInt(UserData.KEY_CURRENCY, 0)
        val penaltyAmount = 50

        prefs.edit()
            .putInt(UserData.KEY_CURRENCY, maxOf(0, currentBalance - penaltyAmount))
            .putInt("REWARDED_HABITS_TODAY", rewardsToday - 1)
            .apply()

        (activity as? MainActivity)?.showTopNotification("Списано $penaltyAmount \uD83C\uDF45")
    }

    private fun readRewardsToday(prefs: SharedPreferences): Int {
        return try {
            prefs.getInt("REWARDED_HABITS_TODAY", 0)
        } catch (e: ClassCastException) {
            prefs.getString("REWARDED_HABITS_TODAY", "")
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.size ?: 0
        }
    }

    private fun applyFailurePenalty() {
        val prefs = requireActivity().getSharedPreferences(UserData.PREFS_NAME, Context.MODE_PRIVATE)
        val currentBalance = prefs.getInt(UserData.KEY_CURRENCY, 0)
        val penalty = 100
        prefs.edit()
            .putInt(UserData.KEY_CURRENCY, maxOf(0, currentBalance - penalty))
            .apply()
    }

    private fun showActionSnackbar(message: String, isSuccess: Boolean) {
        val rootView = view ?: return
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)

        val backgroundColor = ContextCompat.getColor(
            requireContext(),
            if (isSuccess) R.color.habit_good_notification else R.color.habit_bad_notification
        )

        snackbar.setBackgroundTint(backgroundColor)

        val snackbarView = snackbar.view
        val background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_snackbar)
        background?.setTint(backgroundColor)
        snackbarView.background = background

        val params = snackbarView.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            params.leftMargin + 40,
            params.topMargin,
            params.rightMargin + 40,
            params.bottomMargin + 40
        )
        snackbarView.layoutParams = params

        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        textView.compoundDrawablePadding = 24

        val iconRes = if (isSuccess) R.drawable.ic_check_circle else R.drawable.ic_warning
        val icon = ContextCompat.getDrawable(requireContext(), iconRes)
        icon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)

        val bottomNav = activity?.findViewById<View>(R.id.custom_bottom_nav)
        if (bottomNav != null && bottomNav.visibility == View.VISIBLE) {
            snackbar.anchorView = bottomNav
        }

        snackbar.show()
    }

    private fun getTodayDateText(): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

    private fun isToday(dateInMillis: Long): Boolean {
        val date = Calendar.getInstance().apply { timeInMillis = dateInMillis }
        return isSameDay(date, Calendar.getInstance())
    }

    private fun canChangeCalendarDate(habit: Habit, dateInMillis: Long): Boolean {
        val dayStart = getStartOfDay(dateInMillis)
        val createdStart = getStartOfDay(habit.createdAt)
        val todayStart = getStartOfDay(System.currentTimeMillis())
        return dayStart >= createdStart && dayStart <= todayStart
    }

    private fun getStartOfDay(dateInMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
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
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
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
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
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
