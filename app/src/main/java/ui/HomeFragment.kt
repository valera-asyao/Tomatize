package ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.UserData

class HomeFragment : Fragment(), AddHabitDialog.OnHabitAddedListener {

    private lateinit var databaseHelper: HabitDatabaseHelper
    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var habitsRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var mascotOverlayContainer: FrameLayout
    private lateinit var tvCurrencyHome: TextView
    private lateinit var tvMaxStreak: TextView
    private lateinit var tvTomatoHearts: TextView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        mascotOverlayContainer = view.findViewById(R.id.mascot_overlay_container_home)
        habitsRecyclerView = view.findViewById(R.id.habitsRecyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        tvCurrencyHome = view.findViewById(R.id.tvCurrencyHome)
        tvMaxStreak = view.findViewById(R.id.tvMaxStreakTitle)
        tvTomatoHearts = view.findViewById(R.id.tvTomatoHearts)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = HabitDatabaseHelper(requireContext())
        setupRecyclerView()
        loadHabits()
    }

    override fun onResume() {
        super.onResume()
        loadHabits()
        updateMascot()
        updateBalanceUI()
        updateHealthUI()
    }

    private fun updateBalanceUI() {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val balance = prefs.getInt("USER_CURRENCY", 0)
        tvCurrencyHome.text = balance.toString()
    }

    private fun updateHealthUI() {
        val state = TomatoHealthStorage.getState(requireContext())
        val fullHearts = "♥".repeat(state.hearts)
        val emptyHearts = "♡".repeat(TomatoHealthStorage.MAX_HEARTS - state.hearts)
        tvTomatoHearts.text = fullHearts + emptyHearts
    }

    private fun checkAndAwardCurrency(habitId: Long) {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val currentDate = sdf.format(java.util.Date())

        val lastRewardDate = prefs.getString("LAST_REWARD_DATE", "")
        var rewardedHabits = prefs.getString("REWARDED_HABITS_TODAY", "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toMutableList() ?: mutableListOf()
        if (currentDate != lastRewardDate) {
            rewardedHabits = mutableListOf()
            prefs.edit().putString("LAST_REWARD_DATE", currentDate).apply()
        }
        if (rewardedHabits.contains(habitId.toString())) {
            return
        }
        if (rewardedHabits.size < 3) {
            val currentBalance = prefs.getInt("USER_CURRENCY", 0)
            val rewardAmount = 50
            rewardedHabits.add(habitId.toString())
            val newList = rewardedHabits.joinToString(",")
            prefs.edit()
                .putInt("USER_CURRENCY", currentBalance + rewardAmount)
                .putString("REWARDED_HABITS_TODAY", newList)
                .apply()
            android.widget.Toast.makeText(
                requireContext(),
                "Награда $rewardAmount \uD83C\uDF45! (${rewardedHabits.size}/3)",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            android.widget.Toast.makeText(
                requireContext(),
                "Лимит наград на сегодня исчерпан",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkAndDeductCurrency(habitId: Long) {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val rewardedHabitsToday = prefs.getString("REWARDED_HABITS_TODAY", "") ?: ""

        val rewardedList = rewardedHabitsToday.split(",").filter { it.isNotBlank() }.toMutableList()

        if (rewardedList.contains(habitId.toString())) {
            val currentBalance = prefs.getInt("USER_CURRENCY", 0)
            val rewardAmount = 50

            rewardedList.remove(habitId.toString())

            val newList = rewardedList.joinToString(",")

            prefs.edit()
                .putInt("USER_CURRENCY", maxOf(0, currentBalance - rewardAmount))
                .putString("REWARDED_HABITS_TODAY", newList)
                .apply()

            android.widget.Toast.makeText(requireContext(), "Награда $rewardAmount 🍅 отменена", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMascot() {
        val equippedItems = UserData.shopTypes
            .mapNotNull { type -> ShopStorage.getEquippedItemId(requireContext(), type) }
            .mapNotNull(UserData::findItemById)

        MascotOverlayRenderer.render(requireContext(), mascotOverlayContainer, equippedItems)
    }

    private fun setupRecyclerView() {
        habitsAdapter = HabitsAdapter(
            onHabitClick = { habit ->
                showHabitDetails(habit)
            },
            onCompleteClick = { habit ->
                completeHabit(habit)
            },
            onUndoClick = { habit ->
                if (habit.type == HabitType.BAD) {
                    failBadHabit(habit)
                } else {
                    undoHabitCompletion(habit)
                }
            }
        )

        habitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitsAdapter
        }
    }

    private fun loadHabits() {
        val habits = databaseHelper.getAllHabits()
        habitsAdapter.updateHabits(habits)

        val maxStreak = habits.maxOfOrNull {  it.streakCount }?: 0
        tvMaxStreak.text = "$maxStreak"

        if (habits.isEmpty()) {
            emptyStateTextView.visibility = View.VISIBLE
            habitsRecyclerView.visibility = View.GONE
        } else {
            emptyStateTextView.visibility = View.GONE
            habitsRecyclerView.visibility = View.VISIBLE
        }
    }

    fun refreshHabits() {
        loadHabits()
        updateHealthUI()
    }

    fun showAddHabitDialog() {
        val dialog = AddHabitDialog()
        dialog.setOnHabitAddedListener(this)
        dialog.show(parentFragmentManager, "AddHabitDialog")
    }

    override fun onHabitAdded(habit: Habit) {
        val id = databaseHelper.addHabit(habit)
        if (id != -1L) {
            loadHabits()
            android.widget.Toast.makeText(
                requireContext(),
                "Привычка добавлена!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun completeHabit(habit: Habit) {
        val success = databaseHelper.completeHabit(habit.id)
        if (success) {
            loadHabits()
            showCompletionMessage(habit)
            checkAndAwardCurrency(habit.id)
            updateBalanceUI()
            updateHealthUI()
        }
    }

    private fun failBadHabit(habit: Habit) {
        val change = TomatoHealthStorage.loseHearts(requireContext(), habit.heartDamage)
        loadHabits()
        updateHealthUI()

        if (change.died) {
            showTomatoDiedDialog()
        } else {
            android.widget.Toast.makeText(
                requireContext(),
                "Помидор потерял ${habit.heartDamage} серд.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showTomatoDiedDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Помидор погиб")
            .setMessage("Помидор потерял все сердца. Деньги и скины сброшены.")
            .setPositiveButton("OK") { _, _ ->
                updateBalanceUI()
                loadHabits()
                updateMascot()
                updateHealthUI()
            }
            .show()
    }

    private fun undoHabitCompletion(habit: Habit) {
        val success = databaseHelper.undoCompleteHabit(habit.id)
        if (success) {
            checkAndDeductCurrency(habit.id)
            loadHabits()
            updateBalanceUI()
            updateHealthUI()
            android.widget.Toast.makeText(requireContext(), "Выполнение отменено", android.widget.Toast.LENGTH_SHORT).show()
        }
        else {
            android.widget.Toast.makeText(requireContext(), "Привычка сегодня не соблюдалась", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCompletionMessage(habit: Habit) {
        val message = "Привычка ${habit.name} соблюдена!"

        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun deleteHabit(habit: Habit) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Удалить привычку?")
            .setMessage("Вы уверены, что хотите удалить '${habit.name}'? Все данные будут потеряны.")
            .setPositiveButton("Удалить") { _, _ ->
                val success = databaseHelper.deleteHabit(habit.id)
                if (success) {
                    loadHabits()
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Привычка удалена",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Ошибка при удалении",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showHabitDetails(habit: Habit) {
        val view = layoutInflater.inflate(R.layout.dialog_habit_details, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        val description = if (habit.description.isBlank()) "Описание не добавлено" else habit.description
        val typeText = if (habit.type == HabitType.GOOD) "Хорошая привычка" else "Плохая привычка"
        val difficultyText = if (habit.type == HabitType.BAD) "${normalizeBadDifficulty(habit.badDifficulty)}/3" else "нет"
        val lastCompletedText = if (habit.lastCompleted != null) {
            "Последнее выполнение: ${formatDate(habit.lastCompleted)}"
        } else {
            "Еще не выполнялась"
        }
        val typeColor = if (habit.type == HabitType.GOOD) {
            ContextCompat.getColor(requireContext(), R.color.good_habit_color)
        } else {
            ContextCompat.getColor(requireContext(), R.color.bad_habit_color)
        }

        val typeTextView = view.findViewById<TextView>(R.id.tvDetailsType)
        view.findViewById<TextView>(R.id.tvDetailsName).text = habit.name
        typeTextView.text = typeText
        typeTextView.backgroundTintList = ColorStateList.valueOf(typeColor)
        view.findViewById<TextView>(R.id.tvDetailsDescription).text = description
        view.findViewById<TextView>(R.id.tvDetailsStreak).text = "${habit.streakCount} дн."
        view.findViewById<TextView>(R.id.tvDetailsDifficulty).text = difficultyText
        view.findViewById<TextView>(R.id.tvDetailsLastCompleted).text = lastCompletedText

        view.findViewById<Button>(R.id.btnCloseDetails).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnDeleteHabit).setOnClickListener {
            dialog.dismiss()
            deleteHabit(habit)
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialog.show()
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}
