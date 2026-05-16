package org.domir.lessonschedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.domir.lessonschedule.data.local.AppDatabase
import org.domir.lessonschedule.data.local.SettingsRepository

/**
 * Re-schedules all lesson notifications after device reboot
 * or after the app is updated.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context)
                val notificationsEnabled = settings.notificationsEnabled.first()
                if (!notificationsEnabled) return@launch

                val db = AppDatabase.getDatabase(context)
                val lessons = db.scheduleDao().getAllLessons().first()
                LessonNotificationScheduler.scheduleAll(context, lessons)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
