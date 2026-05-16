package org.domir.lessonschedule.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.domir.lessonschedule.R
import org.domir.lessonschedule.data.local.AppDatabase
import org.domir.lessonschedule.data.model.LessonEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleRemoteViewsFactory(this.applicationContext)
    }
}

class ScheduleRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var lessons: List<LessonEntity> = emptyList()

    override fun onCreate() {
        // No heavy ops here
    }

    override fun onDataSetChanged() {
        // Fetch data synchronously
        val db = AppDatabase.getDatabase(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Calendar.getInstance().time)
        
        // Fetch lessons for today and filter unique to avoid duplicates
        val allLessons = db.scheduleDao().getLessonsByDateSync(todayStr)
        lessons = allLessons.distinctBy { Triple(it.dateStart, it.timeStart, it.discipline) }
    }

    override fun onDestroy() {
        lessons = emptyList()
    }

    override fun getCount(): Int = lessons.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= lessons.size) return RemoteViews(context.packageName, R.layout.widget_schedule_item)
        
        val lesson = lessons[position]
        val views = RemoteViews(context.packageName, R.layout.widget_schedule_item)
        
        views.setTextViewText(R.id.itemTime, "${lesson.timeStart} - ${lesson.timeEnd}")
        views.setTextViewText(R.id.itemDiscipline, lesson.discipline)
        
        val roomText = if (!lesson.room.isNullOrBlank()) "Аудитория: ${lesson.room}" else ""
        views.setTextViewText(R.id.itemRoom, roomText)
        
        // Fill intent for item click
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.itemDiscipline, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
