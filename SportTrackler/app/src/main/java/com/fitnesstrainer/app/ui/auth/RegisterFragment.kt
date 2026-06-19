package com.fitnesstrainer.app.ui.auth

import android.os.Bundle
import android.text.InputFilter
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

        val lettersOnly = InputFilter { source, start, end, _, _, _ ->
            source.subSequence(start, end).filter { it.isLetter() || it == '-' || it == ' ' || it == '\'' }
        }
        binding.etFirstName.filters = arrayOf(lettersOnly)
        binding.etLastName.filters  = arrayOf(lettersOnly)

        binding.btnRegister.setOnClickListener {
            val password = binding.etPassword.text.toString()
            val weight   = binding.etWeight.text?.toString()?.toDoubleOrNull()
            val height   = binding.etHeight.text?.toString()?.toDoubleOrNull()

            val error = validatePassword(password)
                ?: validateBody(weight, height)
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val gender = if (binding.rbFemale.isChecked) "Female" else "Male"

            viewModel.register(
                firstName   = binding.etFirstName.text.toString(),
                lastName    = binding.etLastName.text.toString(),
                email       = binding.etEmail.text.toString(),
                password    = password,
                role        = "Client",
                phone       = null,
                trainerCode = null,
                weightKg    = weight,
                heightCm    = height,
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

    private fun validatePassword(pwd: String): String? = when {
        pwd.length < 8                          -> "Пароль должен содержать минимум 8 символов"
        !pwd.any { it.isUpperCase() }           -> "Пароль должен содержать хотя бы одну заглавную букву"
        !pwd.any { it.isLowerCase() }           -> "Пароль должен содержать хотя бы одну строчную букву"
        !pwd.any { !it.isLetterOrDigit() }      -> "Пароль должен содержать хотя бы один специальный символ (!@#\$...)"
        else                                    -> null
    }

    private fun validateBody(weight: Double?, height: Double?): String? = when {
        weight != null && (weight < 20 || weight > 300) -> "Вес должен быть от 20 до 300 кг"
        height != null && (height < 50 || height > 250) -> "Рост должен быть от 50 до 250 см"
        else -> null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
