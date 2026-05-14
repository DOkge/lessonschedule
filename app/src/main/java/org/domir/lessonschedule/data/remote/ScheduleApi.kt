package org.domir.lessonschedule.data.remote

import org.domir.lessonschedule.data.model.GroupsResponse
import org.domir.lessonschedule.data.model.ScheduleResponse
import org.domir.lessonschedule.data.model.YearsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ScheduleApi {
    @GET("api/Rasp")
    suspend fun getSchedule(
        @Query("idGroup") groupId: String,
        @Query("sdate") startDate: String
    ): ScheduleResponse

    @GET("api/Rasp/ListYears")
    suspend fun getYears(): YearsResponse

    @GET("api/raspGrouplist")
    suspend fun getGroups(@Query("year") year: String): GroupsResponse
}
