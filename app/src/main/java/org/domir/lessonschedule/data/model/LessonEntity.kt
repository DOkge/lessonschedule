package org.domir.lessonschedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: Long,
    val dateStart: String,
    val timeStart: String,
    val timeEnd: String,
    val dayOfWeekString: String,
    val dayOfWeek: Int,
    val discipline: String,
    val teacher: String?,
    val room: String?,
    val groupName: String?
)
