package ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R

class HabitsAdapter(
    private var habits: List<Habit> = emptyList(),
    private val onHabitClick: (Habit) -> Unit = {},
    private val onHabitLongClick: (Habit) -> Unit = {},
    private val onCompleteClick: (Habit) -> Unit = {},
    private val onDeleteClick: (Habit) -> Unit = {}
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: View = itemView.findViewById(R.id.habitItemLayout)
        val nameTextView: TextView = itemView.findViewById(R.id.habitNameTextView)
        val streakTextView: TextView = itemView.findViewById(R.id.streakCountTextView)
        val completeButton: Button = itemView.findViewById(R.id.completeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        val context = holder.itemView.context

        holder.nameTextView.text = habit.name
        holder.streakTextView.text = "${habit.streakCount} дн."

        // Apply dynamic colors based on habit type
        val (bgColor, btnColor, textColor) = if (habit.type == HabitType.GOOD) {
            Triple(
                ContextCompat.getColor(context, R.color.habit_good_bg),
                ContextCompat.getColor(context, R.color.habit_good_btn),
                ContextCompat.getColor(context, R.color.habit_good_text)
            )
        } else {
            Triple(
                ContextCompat.getColor(context, R.color.habit_bad_bg),
                ContextCompat.getColor(context, R.color.habit_bad_btn),
                ContextCompat.getColor(context, R.color.habit_bad_text)
            )
        }

        // We can't use triple with IDs directly, using safe colors from resources
        val goodBg = ContextCompat.getColor(context, R.color.habit_good_bg)
        val goodBtn = ContextCompat.getColor(context, R.color.habit_good_btn)
        val goodTxt = ContextCompat.getColor(context, R.color.habit_good_text)

        val badBg = ContextCompat.getColor(context, R.color.habit_bad_bg)
        val badBtn = ContextCompat.getColor(context, R.color.habit_bad_btn)
        val badTxt = ContextCompat.getColor(context, R.color.habit_bad_text)

        if (habit.type == HabitType.GOOD) {
            holder.rootLayout.backgroundTintList = ColorStateList.valueOf(goodBg)
            holder.completeButton.backgroundTintList = ColorStateList.valueOf(goodBtn)
            holder.nameTextView.setTextColor(goodTxt)
            holder.streakTextView.setTextColor(goodTxt)
        } else {
            holder.rootLayout.backgroundTintList = ColorStateList.valueOf(badBg)
            holder.completeButton.backgroundTintList = ColorStateList.valueOf(badBtn)
            holder.nameTextView.setTextColor(badTxt)
            holder.streakTextView.setTextColor(badTxt)
        }

        holder.completeButton.text = "✔"

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