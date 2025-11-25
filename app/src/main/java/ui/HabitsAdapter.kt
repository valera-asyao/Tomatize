package ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import ui.Habit
import java.text.SimpleDateFormat
import java.util.*

class HabitsAdapter(
    private var habits: List<Habit> = emptyList(),
    private val onHabitClick: (Habit) -> Unit = {},
    private val onHabitLongClick: (Habit) -> Unit = {},
    private val onCompleteClick: (Habit) -> Unit = {},
    private val onDeleteClick: (Habit) -> Unit = {}
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.habitNameTextView)

        val streakTextView: TextView = itemView.findViewById(R.id.streakCountTextView)
        val typeIndicator: View = itemView.findViewById(R.id.typeIndicatorView)
        val completeButton: Button = itemView.findViewById(R.id.completeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        holder.nameTextView.text = habit.name
        holder.streakTextView.text = "${habit.streakCount} дн."

        val color = when (habit.type) {
            HabitType.GOOD -> R.color.good_habit_color
            HabitType.BAD -> R.color.bad_habit_color
        }
        holder.typeIndicator.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, color)
        )

        holder.completeButton.text = when (habit.type) {
            HabitType.GOOD -> "✔"
            HabitType.BAD -> "✔"
        }

        holder.completeButton.setOnClickListener {
            onCompleteClick(habit)
        }

        holder.itemView.setOnClickListener {
            onHabitClick(habit)
        }

        holder.itemView.setOnLongClickListener {
            onHabitLongClick(habit)
            true
        }
    }

    override fun getItemCount() = habits.size

    fun updateHabits(newHabits: List<Habit>) {
        habits = newHabits
        notifyDataSetChanged()
    }
}