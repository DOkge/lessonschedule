package org.domir.lessonschedule.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.domir.lessonschedule.MainActivity
import org.domir.lessonschedule.R

class LessonNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "lesson_notifications"
        const val EXTRA_LESSON_TITLE = "extra_lesson_title"
        const val EXTRA_LESSON_ROOM = "extra_lesson_room"
        const val EXTRA_LESSON_TIME = "extra_lesson_time"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_LESSON_TITLE) ?: return
        val room = intent.getStringExtra(EXTRA_LESSON_ROOM) ?: "—"
        val time = intent.getStringExtra(EXTRA_LESSON_TIME) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Занятие через 10 минут")
            .setContentText("$title • каб. $room")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title\nКабинет: $room\nНачало: $time")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Уведомления о занятиях",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминание за 10 минут до начала занятия"
                enableVibration(true)
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
