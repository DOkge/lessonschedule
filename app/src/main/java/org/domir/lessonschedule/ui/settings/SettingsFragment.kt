package org.domir.lessonschedule.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.domir.lessonschedule.ScheduleApplication
import org.domir.lessonschedule.databinding.FragmentSettingsBinding
import org.domir.lessonschedule.ui.MainViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        val app = requireActivity().application as ScheduleApplication
        MainViewModel.Factory(app.scheduleRepository, app.settingsRepository)
    }

    private var selectedGroupId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

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
                        val groupNames = groups.map { it.name }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, groupNames)
                        binding.autoCompleteGroup.setAdapter(adapter)

                        binding.autoCompleteGroup.setOnItemClickListener { _, _, position, _ ->
                            selectedGroupId = groups[position].id.toString()
                        }
                    }
                }
            }
        }

        binding.autoCompleteYear.setOnItemClickListener { _, _, position, _ ->
            val selectedYear = binding.autoCompleteYear.adapter.getItem(position) as String
            binding.autoCompleteGroup.text = null
            selectedGroupId = null
            viewModel.loadGroups(selectedYear)
        }

        binding.btnSaveGroup.setOnClickListener {
            selectedGroupId?.let { groupId ->
                viewModel.setGroupId(groupId)
                Toast.makeText(requireContext(), "Группа сохранена", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } ?: run {
                Toast.makeText(requireContext(), "Выберите группу", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isDarkTheme.collect { isDark ->
                    binding.switchTheme.isChecked = isDark
                }
            }
        }

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                val app = requireActivity().application as ScheduleApplication
                app.settingsRepository.setDarkTheme(isChecked)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val app = requireActivity().application as ScheduleApplication
                app.settingsRepository.notificationsEnabled.collect { enabled ->
                    binding.switchNotifications.isChecked = enabled
                }
            }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                val app = requireActivity().application as ScheduleApplication
                app.settingsRepository.setNotificationsEnabled(isChecked)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
