package ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import com.example.tomatize.ShopStorage
import com.example.tomatize.UserData

class HomeFragment : Fragment(), AddHabitDialog.OnHabitAddedListener {

    private lateinit var databaseHelper: HabitDatabaseHelper
    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var habitsRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var mascotOverlayContainer: FrameLayout
    private lateinit var tvCurrencyHome: TextView

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
        }
    }

    private fun showCompletionMessage(habit: Habit) {
        val message = when (habit.type) {
            HabitType.GOOD -> "Привычка '${habit.name}' выполнена!"
            HabitType.BAD -> "Вы удержались от '${habit.name}'!"
        }
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
        val message = """
            Название: ${habit.name}
            Описание: ${habit.description}
            Тип: ${if (habit.type == HabitType.GOOD) "Хорошая" else "Плохая"}
            Текущий стрик: ${habit.streakCount} дней
            ${if (habit.lastCompleted != null) "Последнее выполнение: ${formatDate(habit.lastCompleted)}" else "Еще не выполнялась"}
        """.trimIndent()

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Информация о привычке")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Удалить") { _, _ ->
                deleteHabit(habit)
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.setTextColor(
                android.graphics.Color.RED
            )
        }

        dialog.show()
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}
