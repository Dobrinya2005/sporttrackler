package com.fitnesstrainer.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Показывать поле кода тренера только при выборе роли «Тренер»
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val isTrainer = checkedId == R.id.rb_trainer
            binding.tilTrainerCode.visibility   = if (isTrainer) View.VISIBLE else View.GONE
            binding.tvTrainerCodeHint.visibility = if (isTrainer) View.VISIBLE else View.GONE
        }

        binding.btnRegister.setOnClickListener {
            val role = if (binding.rbTrainer.isChecked) "Trainer" else "Client"
            val trainerCode = if (role == "Trainer")
                binding.etTrainerCode.text?.toString()?.trim()
            else null
            val gender = if (binding.rbFemale.isChecked) "Female" else "Male"

            viewModel.register(
                firstName   = binding.etFirstName.text.toString(),
                lastName    = binding.etLastName.text.toString(),
                email       = binding.etEmail.text.toString(),
                password    = binding.etPassword.text.toString(),
                role        = role,
                phone       = null,
                trainerCode = trainerCode,
                weightKg    = binding.etWeight.text?.toString()?.toDoubleOrNull(),
                heightCm    = binding.etHeight.text?.toString()?.toDoubleOrNull(),
                gender      = gender
            )
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.progress.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                }
                is AuthState.Success -> {
                    binding.progress.visibility = View.GONE
                    viewModel.resetState()
                    val email    = binding.etEmail.text.toString()
                    val role     = state.role
                    val weightKg = binding.etWeight.text?.toString()?.toFloatOrNull() ?: -1f
                    val heightCm = binding.etHeight.text?.toString()?.toFloatOrNull() ?: -1f
                    val gender   = if (binding.rbFemale.isChecked) "Female" else "Male"
                    val args = android.os.Bundle().apply {
                        putString("email",    email)
                        putString("role",     role)
                        putFloat("weightKg",  weightKg)
                        putFloat("heightCm",  heightCm)
                        putString("gender",   gender)
                        putString("devCode",  state.devCode)
                    }
                    findNavController().navigate(
                        R.id.action_registerFragment_to_emailVerification, args
                    )
                }
                is AuthState.Error -> {
                    binding.progress.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                }
                else -> {
                    binding.progress.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
