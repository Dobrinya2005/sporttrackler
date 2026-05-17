package com.fitnesstrainer.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val storage = App.instance.tokenStorage
            if (storage.isRememberMe()) {
                binding.cbRememberMe.isChecked = true
                binding.etEmail.setText(storage.getSavedEmail())
            }
        }

        binding.btnLogin.onClick = {
            val email    = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            lifecycleScope.launch {
                App.instance.tokenStorage.setRememberMe(binding.cbRememberMe.isChecked, email)
            }
            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.progress.visibility = View.VISIBLE
                    binding.btnLogin.alpha = 0.5f
                }
                is AuthState.Success -> {
                    binding.progress.visibility = View.GONE
                    findNavController().navigate(R.id.action_loginFragment_to_pinFragment)
                }
                is AuthState.Error -> {
                    binding.progress.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.alpha = 1f
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.message
                }
                else -> {
                    binding.progress.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.alpha = 1f
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
