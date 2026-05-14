package org.domir.lessonschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.domir.lessonschedule.data.local.ScheduleDao
import org.domir.lessonschedule.data.local.SettingsRepository
import org.domir.lessonschedule.data.model.LessonEntity
import org.domir.lessonschedule.data.remote.ScheduleApi

class ScheduleRepository(
    private val api: ScheduleApi,
    private val dao: ScheduleDao,
    private val settings: SettingsRepository
) {
    val lessons: Flow<List<LessonEntity>> = dao.getAllLessons()

    suspend fun refreshSchedule(startDate: String) {
        val groupId = settings.groupId.first() ?: return
        try {
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
            dao.deleteAllLessons()
            dao.insertLessons(entities)
        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app we might throw a custom exception to handle errors in UI
            throw e
        }
    }

    suspend fun clearCache() {
        dao.deleteAllLessons()
    }
}
