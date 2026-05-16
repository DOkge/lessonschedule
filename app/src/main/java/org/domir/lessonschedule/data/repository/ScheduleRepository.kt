package org.domir.lessonschedule.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.domir.lessonschedule.data.local.ScheduleDao
import org.domir.lessonschedule.data.local.SettingsRepository
import org.domir.lessonschedule.data.model.LessonEntity
import org.domir.lessonschedule.data.remote.ScheduleApi
import org.domir.lessonschedule.notification.LessonNotificationScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleRepository(
    private val api: ScheduleApi,
    private val dao: ScheduleDao,
    private val settings: SettingsRepository,
    private val context: Context
) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val lessons: Flow<List<LessonEntity>> = dao.getAllLessons()

    suspend fun refreshSchedule(startDate: String) {
        val groupId = settings.groupId.first() ?: return
        val response = api.getSchedule(groupId, startDate)
        val entities = response.data.rasp.map { dto ->
            LessonEntity(
                id = dto.id,
                dateStart = dto.dateStart,
                timeStart = dto.timeStart,
                timeEnd = dto.timeEnd,
                dayOfWeekString = dto.dayOfWeekString,
                dayOfWeek = dto.dayOfWeek,
                discipline = dto.discipline,
                teacher = dto.teacher,
                room = dto.room,
                groupName = dto.group
            )
        }
        // Delete old lessons for this week, then insert fresh data
        // This prevents duplicates when the API returns the same lesson
        // with a different ID on subsequent requests
        val endDate = calculateEndDate(startDate)
        dao.deleteLessonsByDateRange(startDate, endDate)
        dao.insertLessons(entities)

        // Schedule notifications for newly loaded lessons
        scheduleNotifications(entities)
    }

    suspend fun clearCache() {
        // Cancel existing notifications before clearing
        val currentLessons = dao.getAllLessons().first()
        LessonNotificationScheduler.cancelAll(context, currentLessons)
        dao.deleteAllLessons()
    }

    suspend fun getYears(): List<String> {
        return api.getYears().data.years
    }

    suspend fun getGroups(year: String): List<org.domir.lessonschedule.data.model.GroupDto> {
        return api.getGroups(year).data
    }

    private fun calculateEndDate(startDate: String): String {
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(startDate)!!
        cal.add(Calendar.DAY_OF_YEAR, 7)
        return sdf.format(cal.time)
    }

    private suspend fun scheduleNotifications(lessons: List<LessonEntity>) {
        val enabled = settings.notificationsEnabled.first()
        if (enabled) {
            LessonNotificationScheduler.scheduleAll(context, lessons)
        }
    }
}

