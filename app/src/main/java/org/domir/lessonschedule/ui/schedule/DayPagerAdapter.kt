package org.domir.lessonschedule.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.domir.lessonschedule.data.model.LessonEntity
import org.domir.lessonschedule.databinding.FragmentDayPageBinding

class DayPagerAdapter : RecyclerView.Adapter<DayPagerAdapter.DayViewHolder>() {

    private var lessonsByDate: Map<String, List<LessonEntity>> = emptyMap()


    var dateStringForPage: ((Int) -> String)? = null

    fun submitLessons(byDate: Map<String, List<LessonEntity>>) {
        lessonsByDate = byDate
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = org.domir.lessonschedule.ui.MainViewModel.TOTAL_PAGES

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = FragmentDayPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dateStr = dateStringForPage?.invoke(position) ?: return
        val lessons = (lessonsByDate[dateStr] ?: emptyList()).sortedBy { it.timeStart }
        holder.bind(lessons)
    }

    class DayViewHolder(private val binding: FragmentDayPageBinding) : RecyclerView.ViewHolder(binding.root) {
        private val adapter = ScheduleAdapter()

        init {
            binding.recyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            binding.recyclerView.adapter = adapter
        }

        fun bind(lessons: List<LessonEntity>) {
            adapter.submitList(lessons)
            binding.textEmpty.visibility = if (lessons.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (lessons.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
