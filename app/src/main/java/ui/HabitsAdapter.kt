package ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatize.R
import java.util.Calendar

class HabitsAdapter(
    private var habits: List<Habit> = emptyList(),
    private val onHabitClick: (Habit) -> Unit = {},
    private val onHabitLongClick: (Habit) -> Unit = {},
    private val onCompleteClick: (Habit) -> Unit = {},
    private val onDeleteClick: (Habit) -> Unit = {},
    private val onUndoClick: (Habit) -> Unit = {}
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: View = itemView.findViewById(R.id.habitItemLayout)
        val nameTextView: TextView = itemView.findViewById(R.id.habitNameTextView)
        val streakTextView: TextView = itemView.findViewById(R.id.streakCountTextView)
        val completeButton: ImageButton = itemView.findViewById(R.id.completeButton)
        val cancelButton: ImageButton = itemView.findViewById(R.id.cancel_button)
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

        val goodBg = ContextCompat.getColor(context, R.color.habit_good_bg)
        val goodBtn = ContextCompat.getColor(context, R.color.habit_good_btn)
        val goodTxt = ContextCompat.getColor(context, R.color.habit_good_text)

        val badBg = ContextCompat.getColor(context, R.color.red_color)
        val badBtn = ContextCompat.getColor(context, R.color.habit_bad_btn)
        val badTxt = ContextCompat.getColor(context, R.color.habit_bad_text)

        val isDoneToday = isCompletedToday(habit)

        if (habit.type == HabitType.GOOD) {
            holder.rootLayout.backgroundTintList = ColorStateList.valueOf(goodBg)
            holder.completeButton.backgroundTintList = ColorStateList.valueOf(goodBtn)
            holder.cancelButton.backgroundTintList = ColorStateList.valueOf(goodBtn)
            holder.nameTextView.setTextColor(goodTxt)
            holder.streakTextView.setTextColor(goodTxt)
            holder.completeButton.setImageResource(R.drawable.ic_habit_tick)
            
            if (isDoneToday) {
                holder.completeButton.visibility = View.GONE
                holder.cancelButton.visibility = View.VISIBLE
            } else {
                holder.completeButton.visibility = View.VISIBLE
                holder.cancelButton.visibility = View.GONE
            }
        } else {
            holder.rootLayout.backgroundTintList = ColorStateList.valueOf(badBg)
            holder.completeButton.backgroundTintList = ColorStateList.valueOf(badBtn)
            holder.cancelButton.backgroundTintList = ColorStateList.valueOf(badBtn)
            holder.nameTextView.setTextColor(badTxt)
            holder.streakTextView.setTextColor(badTxt)
            holder.completeButton.setImageResource(R.drawable.ic_habit_cross)
            holder.cancelButton.visibility = View.GONE
            
            if (isDoneToday) {
                holder.completeButton.visibility = View.GONE
            } else {
                holder.completeButton.visibility = View.VISIBLE
            }
        }

        holder.completeButton.setOnClickListener {
            onCompleteClick(habit)
        }

        holder.cancelButton.setOnClickListener {
            onUndoClick(habit)
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
        habits = newHabits.sortedWith(
            compareBy<Habit> { it.type }
                .thenBy { it.type == HabitType.GOOD && isCompletedToday(it) }
        )
        notifyDataSetChanged()
    }

    private fun isCompletedToday(habit: Habit): Boolean {
        val lastCompleted = habit.lastCompleted ?: return false
        val habitCalendar = Calendar.getInstance().apply { timeInMillis = lastCompleted }
        val todayCalendar = Calendar.getInstance()
        return habitCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                habitCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
    }
}
