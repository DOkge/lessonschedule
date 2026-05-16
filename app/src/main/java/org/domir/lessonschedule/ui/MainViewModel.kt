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
import java.util.Locale

data class WeekDay(
    val dayOfMonth: Int,
    val shortName: String,
    val dateString: String,
    val isToday: Boolean
)

data class WeekState(
    val days: List<WeekDay>,
    val weekLabel: String,
    val selectedDayIndex: Int
)

class MainViewModel(
    private val repository: ScheduleRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    companion object {
        const val TOTAL_PAGES = 3650
        const val CENTER_PAGE = TOTAL_PAGES / 2
        val SDF = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val DAY_NAMES = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        private val MONTH_NAMES = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
    }

    // Reference date = today at midnight
    private val today: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private val _groupId = MutableStateFlow<String?>(null)
    val groupId = _groupId.asStateFlow()

    private val _isGroupIdLoaded = MutableStateFlow(false)
    val isGroupIdLoaded = _isGroupIdLoaded.asStateFlow()

    val isDarkTheme = settings.isDarkTheme.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false
    )

    val schedule = repository.lessons.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _years = MutableStateFlow<List<String>>(emptyList())
    val years = _years.asStateFlow()

    private val _groups = MutableStateFlow<List<GroupDto>>(emptyList())
    val groups = _groups.asStateFlow()

    // --- Infinite day paging ---
    private var lastPage = CENTER_PAGE
    private val _weekState = MutableStateFlow(buildWeekStateForPage(CENTER_PAGE))
    val weekState = _weekState.asStateFlow()

    // Lessons grouped by date string for fast lookup
    private val _lessonsByDate = MutableStateFlow<Map<String, List<LessonEntity>>>(emptyMap())
    val lessonsByDate = _lessonsByDate.asStateFlow()

    // Track fetched week Mondays to avoid redundant API calls
    private val fetchedWeeks = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            settings.groupId.collect { id ->
                _groupId.value = id
                _isGroupIdLoaded.value = true
                if (!id.isNullOrBlank()) {
                    fetchedWeeks.clear()
                    ensureWeekLoaded(dateForPage(CENTER_PAGE))
                }
            }
        }
        viewModelScope.launch {
            schedule.collect { lessons ->
                _lessonsByDate.value = lessons
                    .distinctBy { Triple(it.dateStart, it.timeStart, it.discipline) }
                    .groupBy { it.dateStart.substringBefore("T") }
            }
        }
    }

    /** Map a ViewPager2 page index to a Calendar date */
    fun dateForPage(page: Int): Calendar {
        return (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, page - CENTER_PAGE)
        }
    }

    fun dateStringForPage(page: Int): String = SDF.format(dateForPage(page).time)

    /** Called by ViewPager2 onPageSelected */
    fun onPageSelected(page: Int) {
        lastPage = page
        _weekState.value = buildWeekStateForPage(page)
        ensureWeekLoaded(dateForPage(page))
    }

    /** Returns the page to navigate to when a day button is clicked */
    fun pageForDayInWeek(dayIndex: Int): Int {
        val currentDayIndex = dayOfWeekIndex(dateForPage(lastPage))
        return lastPage + (dayIndex - currentDayIndex)
    }

    fun getNextWeekPage(): Int = lastPage + 7
    fun getPrevWeekPage(): Int = lastPage - 7

    fun loadYears() {
        viewModelScope.launch {
            try { _years.value = repository.getYears() }
            catch (e: Exception) { _error.value = e.message ?: "Failed to load years" }
        }
    }

    fun loadGroups(year: String) {
        viewModelScope.launch {
            try { _groups.value = repository.getGroups(year) }
            catch (e: Exception) { _error.value = e.message ?: "Failed to load groups" }
        }
    }

    fun setGroupId(id: String) {
        _groupId.value = id
        fetchedWeeks.clear()
        viewModelScope.launch {
            settings.saveGroupId(id)
            repository.clearCache()
        }
    }

    fun clearError() { _error.value = null }

    // --- Private helpers ---

    private fun ensureWeekLoaded(date: Calendar) {
        val monday = getMondayOfWeek(date)
        val key = SDF.format(monday.time)
        if (key !in fetchedWeeks) {
            fetchedWeeks.add(key)
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                try { repository.refreshSchedule(key) }
                catch (e: Exception) { _error.value = e.message ?: "Network error" }
                finally { _isLoading.value = false }
            }
        }
    }

    private fun getMondayOfWeek(date: Calendar): Calendar {
        return (date.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
    }

    private fun dayOfWeekIndex(cal: Calendar): Int {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6; else -> 0
        }
    }

    private fun buildWeekStateForPage(page: Int): WeekState {
        val date = dateForPage(page)
        val selectedDayIndex = dayOfWeekIndex(date)
        val monday = getMondayOfWeek(date)
        val todayStr = SDF.format(today.time)

        val days = (0..6).map { offset ->
            val d = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            WeekDay(
                dayOfMonth = d.get(Calendar.DAY_OF_MONTH),
                shortName = DAY_NAMES[offset],
                dateString = SDF.format(d.time),
                isToday = SDF.format(d.time) == todayStr
            )
        }

        val monthIndex = monday.get(Calendar.MONTH)
        val weekNumber = monday.get(Calendar.WEEK_OF_YEAR)
        val weekLabel = "${days[0].dayOfMonth} ${MONTH_NAMES[monthIndex]}, неделя $weekNumber"

        return WeekState(days, weekLabel, selectedDayIndex)
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
