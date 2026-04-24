package ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
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
    }

    private fun updateBalanceUI() {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val balance = prefs.getInt("USER_CURRENCY", 0)
        tvCurrencyHome.text = balance.toString()
    }

    private fun checkAndAwardCurrency(habitId: Long) {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
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
                if (habit.type == HabitType.BAD) {
                    showBadHabitFailureDialog(habit)
                } else {
                    completeGoodHabit(habit)
                }
            },
            onUndoClick = { habit ->
                undoHabitCompletion(habit)
            }
        )

        habitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitsAdapter
        }
    }

    private fun showBadHabitFailureDialog(habit: Habit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bad_habit_failure, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            if (databaseHelper.recordFailure(habit.id)) {
                loadHabits()
                android.widget.Toast.makeText(requireContext(), "Стрик сброшен", android.widget.Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
    }

    override fun onHabitAdded(habit: Habit) {
        databaseHelper.addHabit(habit)
        loadHabits()
    }

    private fun completeGoodHabit(habit: Habit) {
        val success = databaseHelper.completeHabit(habit.id)
        if (success) {
            loadHabits()
            showCompletionMessage(habit)
            checkAndAwardCurrency(habit.id)
            updateBalanceUI()
        }
    }

    private fun undoHabitCompletion(habit: Habit) {
        val success = databaseHelper.undoCompleteHabit(habit.id)
        if (success) {
            loadHabits()
            updateBalanceUI()
            android.widget.Toast.makeText(requireContext(), "Выполнение отменено", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCompletionMessage(habit: Habit) {
        android.widget.Toast.makeText(requireContext(), "Привычка ${habit.name} соблюдена!", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showHabitDetails(habit: Habit) {
        val fragment = HabitStatisticsFragment.newInstance(habit.id)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}