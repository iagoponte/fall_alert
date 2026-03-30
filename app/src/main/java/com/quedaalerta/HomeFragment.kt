package com.quedaalerta

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.quedaalerta.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()

        binding.btnToggleMonitoring.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("quedaalerta", Context.MODE_PRIVATE)
            val isActive = prefs.getBoolean("monitoring_active", false)
            if (isActive) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
            updateUI()
        }
    }

    private fun startMonitoring() {
        val prefs = requireContext().getSharedPreferences("quedaalerta", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("monitoring_active", true).apply()

        val intent = Intent(requireContext(), FallDetectionService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopMonitoring() {
        val prefs = requireContext().getSharedPreferences("quedaalerta", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("monitoring_active", false).apply()

        val intent = Intent(requireContext(), FallDetectionService::class.java)
        requireContext().stopService(intent)
    }

    private fun updateUI() {
        val prefs = requireContext().getSharedPreferences("quedaalerta", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("monitoring_active", false)

        val contacts = ContactsManager.getContacts(requireContext())

        if (isActive) {
            binding.tvStatus.text = "● Ativo"
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_700))
            binding.cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.green_50)
            )
            binding.btnToggleMonitoring.text = "⏹  Desativar Monitoramento"
            binding.btnToggleMonitoring.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.error)
            )
        } else {
            binding.tvStatus.text = "○ Inativo"
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_600))
            binding.cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.surface)
            )
            binding.btnToggleMonitoring.text = "▶  Ativar Monitoramento"
            binding.btnToggleMonitoring.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
        }

        binding.tvContactCount.text = "${contacts.size} contato(s) cadastrado(s)"

        if (contacts.isEmpty()) {
            binding.tvContactWarning.visibility = View.VISIBLE
        } else {
            binding.tvContactWarning.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
