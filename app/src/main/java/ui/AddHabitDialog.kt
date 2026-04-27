package ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.tomatize.R


class AddHabitDialog : DialogFragment() {

    interface OnHabitAddedListener {
        fun onHabitAdded(habit: Habit)
    }

    private var listener: OnHabitAddedListener? = null
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private var selectedType: HabitType? = null
    private var selectedBadDifficulty = DEFAULT_BAD_DIFFICULTY
    
    private lateinit var btnTypeGood: Button
    private lateinit var btnTypeBad: Button

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
        
        btnTypeGood = view.findViewById(R.id.btnTypeGood)
        btnTypeBad = view.findViewById(R.id.btnTypeBad)

        btnTypeGood.setOnClickListener {
            selectType(HabitType.GOOD)
        }

        btnTypeBad.setOnClickListener {
            selectType(HabitType.BAD)
        }

        val addButton: View = view.findViewById(R.id.btnAddHabit)
        val cancelButton: View = view.findViewById(R.id.btnClose)

        addButton.setOnClickListener {
            Log.d("AddHabitDialog", "Кнопка добавить нажата")
            addHabit()
        }
        cancelButton.setOnClickListener { dismiss() }
    }

    private fun selectType(type: HabitType) {
        selectedType = type
        
        val goodColor = ContextCompat.getColor(requireContext(), R.color.good_habit_color)
        val badColor = ContextCompat.getColor(requireContext(), R.color.bad_habit_color)
        val grayColor = ContextCompat.getColor(requireContext(), R.color.nav_inactive)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)

        if (type == HabitType.GOOD) {
            btnTypeGood.backgroundTintList = ColorStateList.valueOf(goodColor)
            btnTypeGood.setTextColor(whiteColor)
            
            btnTypeBad.backgroundTintList = ColorStateList.valueOf(grayColor)
            btnTypeBad.setTextColor(whiteColor)
        } else {
            btnTypeGood.backgroundTintList = ColorStateList.valueOf(grayColor)
            btnTypeGood.setTextColor(whiteColor)
            
            btnTypeBad.backgroundTintList = ColorStateList.valueOf(badColor)
            btnTypeBad.setTextColor(whiteColor)
        }
    }

    private fun addHabit() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val type = selectedType

        Log.d("AddHabitDialog", "Добавление привычки: name=$name, desc=$description, type=$type")

        if (validateInput(name, description, type)) {
            val habit = Habit(
                name = name,
                description = description,
                type = type!!,
                streakCount = 0,
                lastCompleted = null,
                badDifficulty = if (type == HabitType.BAD) selectedBadDifficulty else DEFAULT_BAD_DIFFICULTY,
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
        selectedType = null
        selectedBadDifficulty = DEFAULT_BAD_DIFFICULTY
        
        val grayColor = ContextCompat.getColor(requireContext(), R.color.nav_inactive)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        
        btnTypeGood.backgroundTintList = ColorStateList.valueOf(grayColor)
        btnTypeGood.setTextColor(whiteColor)
        
        btnTypeBad.backgroundTintList = ColorStateList.valueOf(grayColor)
        btnTypeBad.setTextColor(whiteColor)
        
        nameEditText.requestFocus()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
