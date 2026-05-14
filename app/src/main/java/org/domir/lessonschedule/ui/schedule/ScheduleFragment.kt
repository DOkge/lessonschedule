package org.domir.lessonschedule.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.domir.lessonschedule.R
import org.domir.lessonschedule.ScheduleApplication
import org.domir.lessonschedule.databinding.FragmentScheduleBinding
import org.domir.lessonschedule.ui.MainViewModel

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels {
        val app = requireActivity().application as ScheduleApplication
        MainViewModel.Factory(app.scheduleRepository, app.settingsRepository)
    }

    private lateinit var adapter: ScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ScheduleAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_scheduleFragment_to_settingsFragment)
                    true
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.groupId.collect { groupId ->
                        if (groupId.isNullOrBlank()) {
                            findNavController().navigate(R.id.action_scheduleFragment_to_onboardingFragment)
                        }
                    }
                }

                launch {
                    viewModel.schedule.collect { lessons ->
                        adapter.submitList(lessons)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
