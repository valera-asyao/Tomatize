package ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.tomatize.R
import androidx.appcompat.app.AlertDialog

class EditHabitDialog : DialogFragment() {

    private var habitId: Long = -1
    private var onHabitEditedListener: (() -> Unit)? = null
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var databaseHelper: HabitDatabaseHelper

    companion object {
        private const val ARG_HABIT_ID = "habit_id"

        fun newInstance(habitId: Long): EditHabitDialog {
            val fragment = EditHabitDialog()
            fragment.arguments = Bundle().apply {
                putLong(ARG_HABIT_ID, habitId)
            }
            return fragment
        }
    }

    fun setOnHabitEditedListener(listener: () -> Unit) {
        onHabitEditedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habitId = arguments?.getLong(ARG_HABIT_ID, -1) ?: -1
        databaseHelper = HabitDatabaseHelper(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_habit, null)
        initViews(view)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun initViews(view: View) {
        nameEditText = view.findViewById(R.id.editNameEditText)
        descriptionEditText = view.findViewById(R.id.editDescriptionEditText)

        val habit = databaseHelper.getHabitById(habitId)
        if (habit == null) {
            dismissAllowingStateLoss()
            return
        }

        nameEditText.setText(habit.name)
        descriptionEditText.setText(habit.description)

        view.findViewById<View>(R.id.editHabitContentLayout).setOnClickListener {
            hideKeyboard()
        }

        view.findViewById<Button>(R.id.btnCancelEdit).setOnClickListener {
            hideKeyboard()
            dismiss()
        }

        view.findViewById<Button>(R.id.btnSaveEdit).setOnClickListener {
            hideKeyboard()
            saveHabitChanges()
        }
    }

    private fun saveHabitChanges() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        if (name.isEmpty()) {
            nameEditText.error = "Введите название привычки"
            return
        }

        if (databaseHelper.existsByNameExceptId(name, habitId)) {
            nameEditText.error = "Привычка с таким названием уже существует"
            return
        }

        nameEditText.error = null

        if (databaseHelper.updateHabit(habitId, name, description)) {
            onHabitEditedListener?.invoke()
            dismiss()
        }
    }

    private fun hideKeyboard() {
        val currentView = dialog?.currentFocus ?: dialog?.window?.decorView ?: return
        val manager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(currentView.windowToken, 0)
        currentView.clearFocus()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setGravity(Gravity.CENTER)
    }
}
