package org.domir.lessonschedule.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.domir.lessonschedule.R
import org.domir.lessonschedule.ScheduleApplication
import org.domir.lessonschedule.data.model.GroupDto
import org.domir.lessonschedule.databinding.FragmentOnboardingBinding
import org.domir.lessonschedule.ui.MainViewModel

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        val app = requireActivity().application as ScheduleApplication
        MainViewModel.Factory(app.scheduleRepository, app.settingsRepository)
    }

    private var selectedGroupId: String? = null
    private var groupsList: List<GroupDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadYears()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.years.collect { years ->
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, years)
                        binding.autoCompleteYear.setAdapter(adapter)
                    }
                }

                launch {
                    viewModel.groups.collect { groups ->
                        groupsList = groups
                        val groupNames = groups.map { it.name }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, groupNames)
                        binding.autoCompleteGroup.setAdapter(adapter)
                    }
                }
            }
        }

        binding.autoCompleteYear.setOnItemClickListener { _, _, position, _ ->
            val selectedYear = binding.autoCompleteYear.adapter.getItem(position) as String
            binding.autoCompleteGroup.text = null
            selectedGroupId = null
            binding.btnStart.isEnabled = false
            binding.groupTextInputLayout.isEnabled = false
            
            viewModel.loadGroups(selectedYear)

            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                binding.groupTextInputLayout.isEnabled = true
            }
        }

        // When user selects a group from the filtered dropdown, find its ID by name
        binding.autoCompleteGroup.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            val group = groupsList.find { it.name == selectedName }
            selectedGroupId = group?.id?.toString()
            binding.btnStart.isEnabled = selectedGroupId != null
        }

        binding.btnStart.setOnClickListener {
            selectedGroupId?.let { groupId ->
                viewModel.setGroupId(groupId)
                findNavController().navigate(R.id.action_onboardingFragment_to_scheduleFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
