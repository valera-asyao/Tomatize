package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.tomatize.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class HabitDetailsBottomSheet : BottomSheetDialogFragment() {

    private var habitId: Long = -1
    private var onGoToStatsListener: ((Long) -> Unit)? = null

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

        view.findViewById<TextView>(R.id.detailsHabitName).text = habit.name
        view.findViewById<TextView>(R.id.detailsHabitDescription).text =
            habit.description.ifBlank { "Описание отсутствует" }

        val typeView = view.findViewById<TextView>(R.id.detailsHabitType)
        val statusView = view.findViewById<TextView>(R.id.detailsHabitStatus)

        val isActionToday = habit.lastCompleted?.let {
            val lastCal = Calendar.getInstance().apply { timeInMillis = it }
            val nowCal = Calendar.getInstance()
            lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        } ?: false

        if (habit.type == HabitType.GOOD) {
            typeView.text = "Тип: Хорошая"
            if (isActionToday) {
                statusView.text = "Выполнено"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            } else {
                statusView.text = "Не выполнено"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            }
        } else {
            typeView.text = "Тип: Плохая"
            if (isActionToday) {
                statusView.text = "Срыв зафиксирован"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            } else {
                statusView.text = "Срывов не было"
                statusView.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            }
        }

        view.findViewById<Button>(R.id.btnDetailsClose).setOnClickListener {
            dismiss()
        }

        view.findViewById<TextView>(R.id.btnGoToStats).setOnClickListener {
            dismiss()
            onGoToStatsListener?.invoke(habitId)
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}
