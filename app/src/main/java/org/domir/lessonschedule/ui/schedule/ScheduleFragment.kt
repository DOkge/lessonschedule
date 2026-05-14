package org.domir.lessonschedule.ui.schedule

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.domir.lessonschedule.R
import org.domir.lessonschedule.ScheduleApplication
import org.domir.lessonschedule.databinding.FragmentScheduleBinding
import org.domir.lessonschedule.ui.MainViewModel
import org.domir.lessonschedule.ui.WeekDay

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        val app = requireActivity().application as ScheduleApplication
        MainViewModel.Factory(app.scheduleRepository, app.settingsRepository)
    }

    private lateinit var dayPagerAdapter: DayPagerAdapter
    private val dayButtons = mutableListOf<View>()
    private var suppressPageCallback = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewPager
        dayPagerAdapter = DayPagerAdapter()
        binding.viewPager.adapter = dayPagerAdapter

        // Sync ViewPager swipe → day selector
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!suppressPageCallback) {
                    viewModel.selectDay(position)
                }
            }
        })

        // Toolbar menu
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_scheduleFragment_to_settingsFragment)
                    true
                }
                else -> false
            }
        }

        // Week navigation arrows
        binding.btnPrevWeek.setOnClickListener { viewModel.prevWeek() }
        binding.btnNextWeek.setOnClickListener { viewModel.nextWeek() }

        // Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Navigation guard
                launch {
                    viewModel.isGroupIdLoaded
                        .filter { it }
                        .collect {
                            val currentGroupId = viewModel.groupId.value
                            if (currentGroupId.isNullOrBlank()) {
                                findNavController().navigate(R.id.action_scheduleFragment_to_onboardingFragment)
                            }
                        }
                }

                // Week state (day buttons + header)
                launch {
                    viewModel.weekState.collect { state ->
                        binding.textWeekInfo.text = state.weekLabel
                        buildDayButtons(state.days, state.selectedDayIndex)

                        // Sync pager position
                        if (binding.viewPager.currentItem != state.selectedDayIndex) {
                            suppressPageCallback = true
                            binding.viewPager.setCurrentItem(state.selectedDayIndex, true)
                            suppressPageCallback = false
                        }
                    }
                }

                // Lessons split by day
                launch {
                    viewModel.weekLessons.collect { weekLessons ->
                        dayPagerAdapter.submitWeek(weekLessons)
                    }
                }

                // Loading indicator
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // Error text
                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            binding.errorText.visibility = View.VISIBLE
                            binding.errorText.text = error
                        } else {
                            binding.errorText.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    /**
     * Dynamically builds 7 day-button views inside the dayButtonsContainer.
     * The selected day gets the primary-colored rounded background.
     */
    private fun buildDayButtons(days: List<WeekDay>, selectedIndex: Int) {
        val container = binding.dayButtonsContainer
        container.removeAllViews()
        dayButtons.clear()

        val primaryColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimary)
        val onPrimaryColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        val textColor = resolveThemeColor(android.R.attr.textColorPrimary)
        val textColorSecondary = resolveThemeColor(android.R.attr.textColorSecondary)

        for ((i, day) in days.withIndex()) {
            val isSelected = i == selectedIndex

            // Wrapper for the day button
            val wrapper = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(dpToPx(2), 0, dpToPx(2), 0)
                layoutParams = lp
                setPadding(0, dpToPx(6), 0, dpToPx(6))

                if (isSelected) {
                    setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                    background = ContextCompat.getDrawable(requireContext(), android.R.color.transparent)
                }
            }

            // Card background for selected state
            val card = MaterialCardView(requireContext()).apply {
                val cardLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = cardLp
                radius = dpToPx(12).toFloat()
                cardElevation = 0f
                strokeWidth = 0

                if (isSelected) {
                    setCardBackgroundColor(primaryColor)
                } else {
                    setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }

            val innerLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Day number
            val numberView = TextView(requireContext()).apply {
                text = day.dayOfMonth.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) onPrimaryColor else if (day.isToday) primaryColor else textColor)
            }

            // Day name
            val nameView = TextView(requireContext()).apply {
                text = day.shortName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) onPrimaryColor else textColorSecondary)
            }

            innerLayout.addView(numberView)
            innerLayout.addView(nameView)
            card.addView(innerLayout)

            wrapper.addView(card)
            wrapper.setOnClickListener { viewModel.selectDay(i) }

            container.addView(wrapper)
            dayButtons.add(wrapper)
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        requireContext().theme.resolveAttribute(attr, tv, true)
        return ContextCompat.getColor(requireContext(), tv.resourceId)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
