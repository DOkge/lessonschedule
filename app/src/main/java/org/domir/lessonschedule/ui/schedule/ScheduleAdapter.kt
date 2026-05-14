package org.domir.lessonschedule.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.domir.lessonschedule.data.model.LessonEntity
import org.domir.lessonschedule.databinding.ItemLessonBinding

class ScheduleAdapter : ListAdapter<LessonEntity, ScheduleAdapter.LessonViewHolder>(LessonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LessonViewHolder(private val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lesson: LessonEntity) {
            val dateStr = lesson.dateStart.substringBefore("T") // Simple formatting
            binding.textDay.text = "${lesson.dayOfWeekString}, $dateStr"
            binding.textTime.text = "${lesson.timeStart} - ${lesson.timeEnd}"
            binding.textDiscipline.text = lesson.discipline
            binding.textTeacher.text = lesson.teacher ?: ""
            binding.textRoom.text = lesson.room?.let { "Ауд: $it" } ?: ""
        }
    }

    class LessonDiffCallback : DiffUtil.ItemCallback<LessonEntity>() {
        override fun areItemsTheSame(oldItem: LessonEntity, newItem: LessonEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LessonEntity, newItem: LessonEntity): Boolean {
            return oldItem == newItem
        }
    }
}
