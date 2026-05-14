package org.domir.lessonschedule.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.domir.lessonschedule.R
import org.domir.lessonschedule.ScheduleApplication
import org.domir.lessonschedule.databinding.FragmentOnboardingBinding
import org.domir.lessonschedule.ui.MainViewModel

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels {
        val app = requireActivity().application as ScheduleApplication
        MainViewModel.Factory(app.scheduleRepository, app.settingsRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStart.setOnClickListener {
            val groupId = binding.editGroupId.text.toString()
            if (groupId.isNotBlank()) {
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
