package com.fitnesstrainer.app.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.databinding.FragmentAdminReviewsBinding

class AdminReviewsFragment : Fragment() {

    private var _b: FragmentAdminReviewsBinding? = null
    private val b get() = _b!!
    private val vm: AdminViewModel by viewModels()
    private lateinit var adapter: AdminReviewAdapter
    private var trainerId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentAdminReviewsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trainerId = arguments?.getInt("trainerId") ?: 0
        val trainerName = arguments?.getString("trainerName") ?: "Тренер"
        b.tvTitle.text = "Отзывы: $trainerName"

        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = AdminReviewAdapter(
            onReply = { review ->
                val input = EditText(requireContext()).apply {
                    hint = "Введите ответ..."
                    setText(review.adminReply ?: "")
                    setPadding(40, 20, 40, 20)
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Ответ на отзыв")
                    .setView(input)
                    .setPositiveButton("Сохранить") { _, _ ->
                        val reply = input.text?.toString()?.trim() ?: ""
                        if (reply.isNotEmpty()) vm.replyReview(review.reviewId, reply, trainerId)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            },
            onDelete = { review ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Удалить отзыв")
                    .setMessage("Удалить отзыв от ${review.clientName}?")
                    .setPositiveButton("Удалить") { _, _ -> vm.deleteReview(review.reviewId, trainerId) }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        )

        b.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        b.rvReviews.adapter = adapter

        vm.reviews.observe(viewLifecycleOwner) { adapter.submitList(it) }

        vm.loading.observe(viewLifecycleOwner) { loading ->
            b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        vm.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                vm.clearMessages()
            }
        }

        vm.success.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                vm.clearMessages()
            }
        }

        if (trainerId > 0) vm.loadReviews(trainerId)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
