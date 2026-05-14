package org.domir.lessonschedule

import android.app.Application
import org.domir.lessonschedule.data.local.AppDatabase
import org.domir.lessonschedule.data.local.SettingsRepository
import org.domir.lessonschedule.data.remote.NetworkModule
import org.domir.lessonschedule.data.repository.ScheduleRepository

class ScheduleApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var settingsRepository: SettingsRepository
    lateinit var scheduleRepository: ScheduleRepository

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        scheduleRepository = ScheduleRepository(
            api = NetworkModule.scheduleApi,
            dao = database.scheduleDao(),
            settings = settingsRepository
        )
    }
}
