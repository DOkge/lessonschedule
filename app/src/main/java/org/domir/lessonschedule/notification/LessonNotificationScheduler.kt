package org.domir.lessonschedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.domir.lessonschedule.data.model.LessonEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Schedules exact alarms 10 minutes before each lesson.
 * All alarms are local (AlarmManager) and work without internet.
 */
object LessonNotificationScheduler {

    private const val TAG = "LessonNotifScheduler"
    private const val ADVANCE_MINUTES = 10

    /**
     * Schedule notifications for a list of lessons.
     * Only schedules alarms for future lessons (alarm time > now).
     */
    fun scheduleAll(context: Context, lessons: List<LessonEntity>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        for (lesson in lessons) {
            val alarmTimeMillis = computeAlarmTime(lesson) ?: continue
            if (alarmTimeMillis <= now) continue // skip past lessons

            val requestCode = lesson.id.toInt()

            val intent = Intent(context, LessonNotificationReceiver::class.java).apply {
                putExtra(LessonNotificationReceiver.EXTRA_LESSON_TITLE, lesson.discipline)
                putExtra(LessonNotificationReceiver.EXTRA_LESSON_ROOM, lesson.room ?: "—")
                putExtra(LessonNotificationReceiver.EXTRA_LESSON_TIME, lesson.timeStart)
                putExtra(LessonNotificationReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent
                        )
                    } else {
                        // Fallback to inexact alarm
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot schedule exact alarm: ${e.message}")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent
                )
            }
        }
    }

    /**
     * Cancels all pending alarms for the given lessons.
     */
    fun cancelAll(context: Context, lessons: List<LessonEntity>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (lesson in lessons) {
            val requestCode = lesson.id.toInt()
            val intent = Intent(context, LessonNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Compute the alarm trigger time: lesson start minus 10 minutes.
     * dateStart format: "yyyy-MM-dd" or "yyyy-MM-ddTHH:mm:ss"
     * timeStart format: "HH:mm" or "HH:mm:ss"
     */
    private fun computeAlarmTime(lesson: LessonEntity): Long? {
        return try {
            val datePart = lesson.dateStart.substringBefore("T")
            val timePart = lesson.timeStart.substringBefore(":").let { h ->
                val m = lesson.timeStart.substringAfter(":").substringBefore(":")
                "$h:$m"
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val lessonDate = sdf.parse("$datePart $timePart") ?: return null
            val cal = Calendar.getInstance().apply {
                time = lessonDate
                add(Calendar.MINUTE, -ADVANCE_MINUTES)
            }
            cal.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse lesson time: ${e.message}")
            null
        }
    }
}
