package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.tomatize.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BadHabitFailureBottomSheet : BottomSheetDialogFragment() {

    private var habitId: Long = -1
    private var onConfirmListener: (() -> Unit)? = null

    companion object {
        fun newInstance(habitId: Long): BadHabitFailureBottomSheet {
            val args = Bundle()
            args.putLong("habit_id", habitId)
            val fragment = BadHabitFailureBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    fun setOnConfirmListener(listener: () -> Unit) {
        onConfirmListener = listener
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
        return inflater.inflate(R.layout.dialog_bad_habit_failure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val habit = HabitDatabaseHelper(requireContext()).getHabitById(habitId)
        if (habit != null) {
            view.findViewById<TextView>(R.id.dialogMessage).text =
                "Стрик будет сброшен. Помидор потеряет ${habit.heartDamage} серд."
        }

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }

        view.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            onConfirmListener?.invoke()
            dismiss()
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}
