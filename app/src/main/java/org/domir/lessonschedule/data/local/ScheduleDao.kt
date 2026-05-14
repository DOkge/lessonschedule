package org.domir.lessonschedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.domir.lessonschedule.data.model.LessonEntity

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM lessons ORDER BY dateStart ASC, timeStart ASC")
    fun getAllLessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}
