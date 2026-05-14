package org.domir.lessonschedule.data.remote

import org.domir.lessonschedule.data.model.ScheduleResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ScheduleApi {
    @GET("api/Rasp")
    suspend fun getSchedule(
        @Query("idGroup") groupId: String,
        @Query("sdate") startDate: String
    ): ScheduleResponse
}
