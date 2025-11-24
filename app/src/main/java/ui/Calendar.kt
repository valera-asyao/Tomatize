package ui

import java.util.Calendar
import java.util.Locale

class Calendar {

    //Получение начала недели (понедельник) для указанной даты
    fun getStartOfWeek(calendar: Calendar = Calendar.getInstance()): Calendar {
        val startOfWeek = calendar.clone() as Calendar
        startOfWeek.firstDayOfWeek = Calendar.MONDAY
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        startOfWeek.set(Calendar.MINUTE, 0)
        startOfWeek.set(Calendar.SECOND, 0)
        startOfWeek.set(Calendar.MILLISECOND, 0)
        return startOfWeek
    }


    fun getCurrentWeekDays(): List<Calendar> {
        val weekDays = mutableListOf<Calendar>()
        val startOfWeek = getStartOfWeek()

        for (i in 0 until 7) {
            val day = startOfWeek.clone() as Calendar
            day.add(Calendar.DAY_OF_MONTH, i)
            weekDays.add(day)
        }

        return weekDays
    }

    fun getMonthCalendarDays(year: Int, month: Int): List<Calendar?> {
        val days = mutableListOf<Calendar?>()

        val firstDayOfMonth = Calendar.getInstance().apply {
            set(year, month, 1)
        }

        val lastDayOfMonth = Calendar.getInstance().apply {
            set(year, month, getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        // Пустые дни до начала месяца
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
        val emptyDaysBefore = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        repeat(emptyDaysBefore) {
            days.add(null)
        }

        // Дни месяца
        val tempCalendar = firstDayOfMonth.clone() as Calendar
        while (tempCalendar.get(Calendar.MONTH) == month) {
            days.add(tempCalendar.clone() as Calendar)
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Пустые дни после конца месяца (до 42 ячеек)
        while (days.size < 42) {
            days.add(null)
        }

        return days
    }

    //Проверка, является ли день сегодняшним
    fun isToday(calendar: Calendar): Boolean {
        return isSameDay(calendar, Calendar.getInstance())
    }

    //Проверка, являются ли две даты одним днем
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    //Получение названия дня недели
    fun getDayOfWeekName(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Пн"
            Calendar.TUESDAY -> "Вт"
            Calendar.WEDNESDAY -> "Ср"
            Calendar.THURSDAY -> "Чт"
            Calendar.FRIDAY -> "Пт"
            Calendar.SATURDAY -> "Сб"
            Calendar.SUNDAY -> "Вс"
            else -> ""
        }
    }

    //Получение названия месяца
    fun getMonthName(month: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
        }
        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
    }

    //Форматирование даты для отображения
    fun formatDate(calendar: Calendar, pattern: String = "dd.MM.yyyy"): String {
        val formatter = java.text.SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(calendar.time)
    }

    //Получение начала дня (00:00:00)
    fun getStartOfDay(calendar: Calendar): Calendar {
        val startOfDay = calendar.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)
        return startOfDay
    }

    //Получение конца дня (23:59:59)
    fun getEndOfDay(calendar: Calendar): Calendar {
        val endOfDay = calendar.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59)
        endOfDay.set(Calendar.MILLISECOND, 999)
        return endOfDay
    }

    //Проверка, находится ли дата в текущем месяце
    fun isInCurrentMonth(calendar: Calendar): Boolean {
        val now = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }

    //Получение количества дней между двумя датами
    fun getDaysBetween(start: Calendar, end: Calendar): Int {
        val startCopy = start.clone() as Calendar
        val endCopy = end.clone() as Calendar

        getStartOfDay(startCopy)
        getStartOfDay(endCopy)

        val diff = endCopy.timeInMillis - startCopy.timeInMillis
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }
}