package com.fitnesstrainer.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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

        binding.btnRegister.setOnClickListener {
            val role = if (binding.rbTrainer.isChecked) "Trainer" else "Client"
            viewModel.register(
                firstName = binding.etFirstName.text.toString(),
                lastName  = binding.etLastName.text.toString(),
                email     = binding.etEmail.text.toString(),
                password  = binding.etPassword.text.toString(),
                role      = role,
                phone     = null
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
                }
                is AuthState.Success -> {
                    binding.progress.visibility = View.GONE
                    val action = if (state.role == "Trainer")
                        R.id.action_registerFragment_to_trainerDashboardFragment
                    else
                        R.id.action_registerFragment_to_clientDashboardFragment
                    findNavController().navigate(action)
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
