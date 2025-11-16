package ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.tomatize.R


class AddHabitDialog : DialogFragment() {

    interface OnHabitAddedListener {
        fun onHabitAdded(habit: Habit)
    }

    private var listener: OnHabitAddedListener? = null
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var goodRadioButton: RadioButton
    private lateinit var badRadioButton: RadioButton

    fun setOnHabitAddedListener(listener: OnHabitAddedListener) {
        this.listener = listener
        Log.d("AddHabitDialog", "Listener установлен: ${listener != null}")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.fragment_add_habit, null)

        initViews(view)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun initViews(view: View) {
        nameEditText = view.findViewById(R.id.nameEditText)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        typeRadioGroup = view.findViewById(R.id.typeRadioGroup)
        goodRadioButton = view.findViewById(R.id.goodRadioButton)
        badRadioButton = view.findViewById(R.id.badRadioButton)

        val addButton: View = view.findViewById(R.id.btnAddHabit)
        val cancelButton: View = view.findViewById(R.id.btnClose)

        addButton.setOnClickListener {
            Log.d("AddHabitDialog", "Кнопка добавить нажата")
            addHabit()
        }
        cancelButton.setOnClickListener { dismiss() }
    }

    private fun addHabit() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val type = when (typeRadioGroup.checkedRadioButtonId) {
            R.id.goodRadioButton -> HabitType.GOOD
            R.id.badRadioButton -> HabitType.BAD
            else -> null
        }

        Log.d("AddHabitDialog", "Добавление привычки: name=$name, desc=$description, type=$type")

        if (validateInput(name, description, type)) {
            val habit = Habit(
                name = name,
                description = description,
                type = type!!,
                streakCount = 0,
                lastCompleted = null,
                createdAt = System.currentTimeMillis()
            )

            Log.d("AddHabitDialog", "Валидация прошла, вызываем listener: ${listener != null}")
            listener?.onHabitAdded(habit)
            dismiss()
        } else {
            Log.d("AddHabitDialog", "Валидация не прошла")
        }
    }

    private fun validateInput(name: String, description: String, type: HabitType?): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            nameEditText.error = "Введите название привычки"
            isValid = false
        } else {
            nameEditText.error = null
        }

        if (description.isEmpty()) {
            descriptionEditText.error = "Введите описание привычки"
            isValid = false
        } else {
            descriptionEditText.error = null
        }

        if (type == null) {
            Toast.makeText(requireContext(), "Выберите тип привычки", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    override fun onStart() {
        super.onStart()
        nameEditText.text?.clear()
        descriptionEditText.text?.clear()
        typeRadioGroup.clearCheck()
        nameEditText.requestFocus()

        }
    }