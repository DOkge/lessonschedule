package org.domir.lessonschedule

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.domir.lessonschedule.data.local.AppDatabase
import org.domir.lessonschedule.data.local.SettingsRepository
import org.domir.lessonschedule.data.remote.NetworkModule
import org.domir.lessonschedule.data.repository.ScheduleRepository
import org.domir.lessonschedule.notification.LessonNotificationReceiver

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
            settings = settingsRepository,
            context = applicationContext
        )
//        createNotificationChannel()
    }

    //Инициализация уведомлений
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                LessonNotificationReceiver.CHANNEL_ID,
//                "Уведомления о занятиях",
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "Напоминание за 10 минут до начала занятия"
//                enableVibration(true)
//            }
//            val manager = getSystemService(NotificationManager::class.java)
//            manager.createNotificationChannel(channel)
//        }
//    }
}

