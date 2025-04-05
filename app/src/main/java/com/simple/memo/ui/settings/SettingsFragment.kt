package com.simple.memo.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simple.memo.R
import com.simple.memo.databinding.FragmentSettingsBinding
import androidx.core.content.edit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_NAME = "settings_prefs"
        private const val KEY_TEXT_SIZE = "key_text_size"
        private const val KEY_AUTO_DELETE = "key_auto_delete"
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

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        when (prefs.getString(KEY_TEXT_SIZE, "medium")) {
            "small" -> binding.textSizeToggleGroup.check(R.id.btn_small)
            "medium" -> binding.textSizeToggleGroup.check(R.id.btn_medium)
            "large" -> binding.textSizeToggleGroup.check(R.id.btn_large)
        }

        when (prefs.getString(KEY_AUTO_DELETE, "never")) {
            "never" -> binding.deleteCycleToggleGroup.check(R.id.btn_never)
            "7days" -> binding.deleteCycleToggleGroup.check(R.id.btn_7days)
            "30days" -> binding.deleteCycleToggleGroup.check(R.id.btn_30days)
        }

        binding.textSizeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val value = when (checkedId) {
                    R.id.btn_small -> "small"
                    R.id.btn_medium -> "medium"
                    R.id.btn_large -> "large"
                    else -> "medium"
                }
                prefs.edit { putString(KEY_TEXT_SIZE, value) }
            }
        }

        binding.deleteCycleToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val value = when (checkedId) {
                    R.id.btn_never -> "never"
                    R.id.btn_7days -> "7days"
                    R.id.btn_30days -> "30days"
                    else -> "never"
                }
                prefs.edit { putString(KEY_AUTO_DELETE, value) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
