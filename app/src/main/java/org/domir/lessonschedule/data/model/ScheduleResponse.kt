package org.domir.lessonschedule.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ScheduleResponse(
    val data: ScheduleData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ScheduleData(
    val rasp: List<LessonDto>
)

@SuppressLint("UnsafeOptInUsageError")
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

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class YearsResponse(
    val data: YearsData
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class YearsData(
    val years: List<String>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GroupsResponse(
    val data: List<GroupDto>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GroupDto(
    val id: Long,
    val name: String
)
