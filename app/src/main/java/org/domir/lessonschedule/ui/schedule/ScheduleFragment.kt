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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewPager2 with infinite-like paging
        dayPagerAdapter = DayPagerAdapter()
        dayPagerAdapter.dateStringForPage = { page -> viewModel.dateStringForPage(page) }
        binding.viewPager.adapter = dayPagerAdapter
        binding.viewPager.setCurrentItem(MainViewModel.CENTER_PAGE, false)

        // ViewPager page change → update ViewModel state
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onPageSelected(position)
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
        binding.btnPrevWeek.setOnClickListener {
            binding.viewPager.setCurrentItem(viewModel.getPrevWeekPage(), true)
        }
        binding.btnNextWeek.setOnClickListener {
            binding.viewPager.setCurrentItem(viewModel.getNextWeekPage(), true)
        }

        // Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Navigation guard
                launch {
                    viewModel.isGroupIdLoaded.filter { it }.collect {
                        if (viewModel.groupId.value.isNullOrBlank()) {
                            findNavController().navigate(R.id.action_scheduleFragment_to_onboardingFragment)
                        }
                    }
                }

                // Week state → rebuild day buttons + header
                launch {
                    viewModel.weekState.collect { state ->
                        binding.textWeekInfo.text = state.weekLabel
                        buildDayButtons(state.days, state.selectedDayIndex)
                    }
                }

                // Lessons by date → adapter
                launch {
                    viewModel.lessonsByDate.collect { byDate ->
                        dayPagerAdapter.submitLessons(byDate)
                    }
                }

                // Loading
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                // Error
                launch {
                    viewModel.error.collect { err ->
                        binding.errorText.visibility = if (err != null) View.VISIBLE else View.GONE
                        binding.errorText.text = err ?: ""
                    }
                }
            }
        }
    }

    private fun buildDayButtons(days: List<WeekDay>, selectedIndex: Int) {
        val container = binding.dayButtonsContainer
        container.removeAllViews()

        val primaryColor = resolveColor(com.google.android.material.R.attr.colorPrimary)
        val onPrimaryColor = resolveColor(com.google.android.material.R.attr.colorOnPrimary)
        val textColor = resolveColor(android.R.attr.textColorPrimary)
        val textSecondary = resolveColor(android.R.attr.textColorSecondary)

        for ((i, day) in days.withIndex()) {
            val selected = i == selectedIndex

            val card = MaterialCardView(requireContext()).apply {
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(dpToPx(2), 0, dpToPx(2), 0)
                layoutParams = lp
                radius = dpToPx(12).toFloat()
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(if (selected) primaryColor else android.graphics.Color.TRANSPARENT)
                setOnClickListener {
                    val targetPage = viewModel.pageForDayInWeek(i)
                    binding.viewPager.setCurrentItem(targetPage, true)
                }
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, dpToPx(8))
            }

            inner.addView(TextView(requireContext()).apply {
                text = day.dayOfMonth.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(if (selected) onPrimaryColor else if (day.isToday) primaryColor else textColor)
            })

            inner.addView(TextView(requireContext()).apply {
                text = day.shortName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
                setTextColor(if (selected) onPrimaryColor else textSecondary)
            })

            card.addView(inner)
            container.addView(card)
        }
    }

    private fun resolveColor(attr: Int): Int {
        val tv = TypedValue()
        requireContext().theme.resolveAttribute(attr, tv, true)
        return ContextCompat.getColor(requireContext(), tv.resourceId)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
