package org.domir.lessonschedule.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import org.domir.lessonschedule.R
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
            binding.textTime.text = "${lesson.timeStart} - ${lesson.timeEnd}"
            binding.textDiscipline.text = lesson.discipline
            binding.textTeacher.text = lesson.teacher ?: ""
            binding.textRoom.text = lesson.room?.let { "Ауд: $it" } ?: ""

            val lower = lesson.discipline.lowercase()
            val colorRes = when {
                "лаб" in lower -> R.color.lesson_bg_lab
                "практ" in lower || "пр." in lower || "(пр)" in lower || lower.startsWith("пр ") || " пр " in lower -> R.color.lesson_bg_practice
                "лекц" in lower || "лек." in lower || "(лек)" in lower || lower.startsWith("лек ") || " лек " in lower -> R.color.lesson_bg_lecture
                else -> R.color.lesson_bg_default
            }
            binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, colorRes))
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
