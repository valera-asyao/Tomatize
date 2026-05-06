package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.tomatize.MainActivity
import com.example.tomatize.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class HabitDetailsBottomSheet : BottomSheetDialogFragment() {

    private var habitId: Long = -1
    private var onGoToStatsListener: ((Long) -> Unit)? = null
    private var onHabitDeletedListener: (() -> Unit)? = null
    private var onHabitEditedListener: (() -> Unit)? = null

    companion object {
        fun newInstance(habitId: Long): HabitDetailsBottomSheet {
            val args = Bundle()
            args.putLong("habit_id", habitId)
            val fragment = HabitDetailsBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    fun setOnGoToStatsListener(listener: (Long) -> Unit) {
        onGoToStatsListener = listener
    }

    fun setOnHabitDeletedListener(listener: () -> Unit) {
        onHabitDeletedListener = listener
    }

    fun setOnHabitEditedListener(listener: () -> Unit) {
        onHabitEditedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habitId = arguments?.getLong("habit_id") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_habit_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val databaseHelper = HabitDatabaseHelper(requireContext())
        val habit = databaseHelper.getHabitById(habitId) ?: return

        fun updateEditableHabitFields(updatedHabit: Habit) {
            view.findViewById<TextView>(R.id.detailsHabitName).text = updatedHabit.name
            view.findViewById<TextView>(R.id.detailsHabitDescription).text =
                updatedHabit.description.ifBlank { "Описание отсутствует" }
        }

        updateEditableHabitFields(habit)

        val typeView = view.findViewById<TextView>(R.id.detailsHabitType)
        val difficultyView = view.findViewById<TextView>(R.id.detailsHabitDifficulty)
        val statusView = view.findViewById<TextView>(R.id.detailsHabitStatus)

        val isActionToday = habit.lastCompleted?.let {
            val lastCal = Calendar.getInstance().apply { timeInMillis = it }
            val nowCal = Calendar.getInstance()
            lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        } ?: false

        if (habit.type == HabitType.GOOD) {
            typeView.text = "Тип: Хорошая"
            difficultyView.text = "Сложность: нет"
            if (isActionToday) {
                statusView.text = "Выполнено"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            } else {
                statusView.text = "Не выполнено"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            }
        } else {
            typeView.text = "Тип: Плохая"
            difficultyView.text = "Сложность: ${normalizeBadDifficulty(habit.badDifficulty)}/3"
            if (isActionToday) {
                statusView.text = "Срыв зафиксирован"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            } else {
                statusView.text = "Срывов не было"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            }
        }

        view.findViewById<ImageButton>(R.id.deleteButton).setOnClickListener {
            showDeleteConfirmation()
        }

        view.findViewById<ImageButton>(R.id.editButton).setOnClickListener {
            val editDialog = EditHabitDialog.newInstance(habitId)
            editDialog.setOnHabitEditedListener {
                databaseHelper.getHabitById(habitId)?.let { updatedHabit ->
                    updateEditableHabitFields(updatedHabit)
                }
                onHabitEditedListener?.invoke()
                (activity as? MainActivity)?.showTopNotification("Привычка обновлена")
            }
            editDialog.show(parentFragmentManager, "EditHabitDialog")
        }

        view.findViewById<Button>(R.id.btnDetailsClose).setOnClickListener {
            dismiss()
        }

        view.findViewById<TextView>(R.id.btnGoToStats).setOnClickListener {
            dismiss()
            onGoToStatsListener?.invoke(habitId)
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
            val databaseHelper = HabitDatabaseHelper(requireContext())
            if (databaseHelper.deleteHabit(habitId)) {
                (activity as? MainActivity)?.showTopNotification("Привычка удалена")
                onHabitDeletedListener?.invoke()
                dialog.dismiss()
                dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}
