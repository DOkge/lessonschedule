package org.domir.lessonschedule.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.domir.lessonschedule.data.model.LessonEntity
import org.domir.lessonschedule.databinding.FragmentDayPageBinding

/**
 * ViewPager2 adapter — each page shows lessons for one day.
 * We use RecyclerView.Adapter (not FragmentStateAdapter) because
 * the content is lightweight and we need fast date switching.
 */
class DayPagerAdapter : RecyclerView.Adapter<DayPagerAdapter.DayViewHolder>() {

    // List of 7 lists (Mon..Sun), each containing that day's lessons
    private var weekLessons: List<List<LessonEntity>> = List(7) { emptyList() }

    fun submitWeek(lessons: List<List<LessonEntity>>) {
        weekLessons = lessons
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 7

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = FragmentDayPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(weekLessons[position])
    }

    class DayViewHolder(private val binding: FragmentDayPageBinding) : RecyclerView.ViewHolder(binding.root) {
        private val adapter = ScheduleAdapter()

        init {
            binding.recyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            binding.recyclerView.adapter = adapter
        }

        fun bind(lessons: List<LessonEntity>) {
            adapter.submitList(lessons)
            if (lessons.isEmpty()) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }
}
