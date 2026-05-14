package org.domir.lessonschedule.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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

    private val viewModel: MainViewModel by viewModels {
        val app = requireActivity().application as ScheduleApplication
        MainViewModel.Factory(app.scheduleRepository, app.settingsRepository)
    }

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupId.collect { groupId ->
                    if (binding.editGroupId.text.toString() != groupId) {
                        binding.editGroupId.setText(groupId)
                    }
                }
            }
        }

        binding.btnSaveGroup.setOnClickListener {
            val newGroupId = binding.editGroupId.text.toString()
            if (newGroupId.isNotBlank()) {
                viewModel.setGroupId(newGroupId)
                Toast.makeText(requireContext(), "Группа сохранена", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
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
