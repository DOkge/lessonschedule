package org.domir.lessonschedule.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val data: ScheduleData
)

@Serializable
data class ScheduleData(
    val rasp: List<LessonDto>
)

@Serializable
data class LessonDto(
    @SerialName("код") val id: Long,
    @SerialName("датаНачала") val dateStart: String,
    @SerialName("начало") val timeStart: String,
    @SerialName("конец") val timeEnd: String,
    @SerialName("день_недели") val dayOfWeekString: String,
    @SerialName("деньНедели") val dayOfWeek: Int,
    @SerialName("дисциплина") val discipline: String,
    @SerialName("преподаватель") val teacher: String? = null,
    @SerialName("аудитория") val room: String? = null,
    @SerialName("группа") val group: String? = null
)
