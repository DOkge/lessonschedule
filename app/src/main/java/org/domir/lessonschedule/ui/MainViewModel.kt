package org.domir.lessonschedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.domir.lessonschedule.data.local.SettingsRepository
import org.domir.lessonschedule.data.model.GroupDto
import org.domir.lessonschedule.data.model.LessonEntity
import org.domir.lessonschedule.data.repository.ScheduleRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeekDay(
    val dayOfMonth: Int,
    val shortName: String,       // "Пн", "Вт", …
    val dateString: String,      // "yyyy-MM-dd"
    val isToday: Boolean
)

data class WeekState(
    val days: List<WeekDay>,
    val weekLabel: String,       // "12 мая, неделя 20"
    val selectedDayIndex: Int    // 0-6
)

class MainViewModel(
    private val repository: ScheduleRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    companion object {
        private val DAY_NAMES = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        private val MONTH_NAMES = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        private val SDF = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    private val _groupId = MutableStateFlow<String?>(null)
    val groupId = _groupId.asStateFlow()

    private val _isGroupIdLoaded = MutableStateFlow(false)
    val isGroupIdLoaded = _isGroupIdLoaded.asStateFlow()

    val isDarkTheme = settings.isDarkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val schedule = repository.lessons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _years = MutableStateFlow<List<String>>(emptyList())
    val years = _years.asStateFlow()

    private val _groups = MutableStateFlow<List<GroupDto>>(emptyList())
    val groups = _groups.asStateFlow()

    // --- Week / day navigation ---
    // The Monday of the currently displayed week
    private val currentWeekMonday = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val _weekState = MutableStateFlow(buildWeekState(todayIndex()))
    val weekState = _weekState.asStateFlow()

    // Lessons split by day index (0 = Mon .. 6 = Sun) for the current week
    private val _weekLessons = MutableStateFlow<List<List<LessonEntity>>>(List(7) { emptyList() })
    val weekLessons = _weekLessons.asStateFlow()

    init {
        viewModelScope.launch {
            settings.groupId.collect { id ->
                _groupId.value = id
                _isGroupIdLoaded.value = true
                if (!id.isNullOrBlank()) {
                    refresh()
                }
            }
        }

        // React to schedule changes and split into days
        viewModelScope.launch {
            schedule.collect { lessons ->
                splitLessonsByDay(lessons)
            }
        }
    }

    /** Select a specific day index (0-6) within the current week */
    fun selectDay(index: Int) {
        _weekState.value = buildWeekState(index.coerceIn(0, 6))
    }

    /** Move to next week, keeping the same day-of-week selected */
    fun nextWeek() {
        currentWeekMonday.add(Calendar.WEEK_OF_YEAR, 1)
        _weekState.value = buildWeekState(_weekState.value.selectedDayIndex)
        refresh()
    }

    /** Move to previous week, keeping the same day-of-week selected */
    fun prevWeek() {
        currentWeekMonday.add(Calendar.WEEK_OF_YEAR, -1)
        _weekState.value = buildWeekState(_weekState.value.selectedDayIndex)
        refresh()
    }

    fun loadYears() {
        viewModelScope.launch {
            try {
                _years.value = repository.getYears()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load years"
            }
        }
    }

    fun loadGroups(year: String) {
        viewModelScope.launch {
            try {
                _groups.value = repository.getGroups(year)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load groups"
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val mondayDate = SDF.format(currentWeekMonday.time)
                repository.refreshSchedule(mondayDate)
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setGroupId(id: String) {
        _groupId.value = id
        viewModelScope.launch {
            settings.saveGroupId(id)
            repository.clearCache()
        }
    }

    fun clearError() {
        _error.value = null
    }

    // ---- Private helpers ----

    private fun todayIndex(): Int {
        val cal = Calendar.getInstance()
        // Calendar.MONDAY = 2, we want 0-based Mon=0
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return when (dow) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    private fun buildWeekState(selectedIndex: Int): WeekState {
        val today = Calendar.getInstance()
        val todayStr = SDF.format(today.time)

        val days = (0..6).map { offset ->
            val dayCal = currentWeekMonday.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, offset)
            val dateStr = SDF.format(dayCal.time)
            WeekDay(
                dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH),
                shortName = DAY_NAMES[offset],
                dateString = dateStr,
                isToday = dateStr == todayStr
            )
        }

        val mondayDay = days[0].dayOfMonth
        val monthIndex = currentWeekMonday.get(Calendar.MONTH)
        val weekNumber = currentWeekMonday.get(Calendar.WEEK_OF_YEAR)
        val weekLabel = "$mondayDay ${MONTH_NAMES[monthIndex]}, неделя $weekNumber"

        return WeekState(days, weekLabel, selectedIndex)
    }

    private fun splitLessonsByDay(allLessons: List<LessonEntity>) {
        // Build a map of dateString -> lessons for the current week
        val weekDays = _weekState.value.days
        val byDate = allLessons.groupBy { it.dateStart.substringBefore("T") }

        val split = weekDays.map { day ->
            (byDate[day.dateString] ?: emptyList()).sortedBy { it.timeStart }
        }
        _weekLessons.value = split
    }

    class Factory(
        private val repository: ScheduleRepository,
        private val settings: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, settings) as T
        }
    }
}
