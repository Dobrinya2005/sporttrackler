package com.fitnesstrainer.app.ui.food

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.FoodProduct
import com.fitnesstrainer.app.databinding.DialogAddFoodBinding
import com.fitnesstrainer.app.databinding.DialogFoodWeightBinding
import com.fitnesstrainer.app.databinding.FragmentFoodDiaryBinding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.MealSummary
import kotlinx.coroutines.launch
import com.fitnesstrainer.app.util.FoodEmojiUtil
import com.fitnesstrainer.app.util.PdfExporter
import java.time.format.DateTimeFormatter

class FoodDiaryFragment : Fragment() {

    private var _binding: FragmentFoodDiaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FoodDiaryViewModel by viewModels()
    private lateinit var diaryAdapter: FoodDiaryAdapter
    private lateinit var searchAdapter: FoodSearchAdapter
    private var pendingMealType = "Breakfast"
    private var currentMeals: List<MealSummary> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diaryAdapter = FoodDiaryAdapter(
            onDelete   = { diaryId -> viewModel.deleteEntry(diaryId) },
            onAddClick = { mealType -> showAddFoodDialog(mealType) }
        )
        binding.rvDiary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDiary.adapter       = diaryAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                return if (diaryAdapter.isSwipeable(vh.adapterPosition))
                    ItemTouchHelper.LEFT else 0
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val diaryId = diaryAdapter.getDiaryIdAt(vh.adapterPosition)
                if (diaryId != null) viewModel.deleteEntry(diaryId)
            }
        }).attachToRecyclerView(binding.rvDiary)

        binding.btnBack.setOnClickListener  { findNavController().popBackStack() }
        binding.btnPrevDay.setOnClickListener { viewModel.previousDay() }
        binding.btnNextDay.setOnClickListener { viewModel.nextDay() }

        binding.btnExportPdf.setOnClickListener {
            if (currentMeals.isEmpty() || currentMeals.all { it.items.isEmpty() }) {
                Toast.makeText(requireContext(), "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dateStr = viewModel.currentDate.value
                ?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: ""
            lifecycleScope.launch {
                val firstName = App.instance.tokenStorage.getFirstName() ?: ""
                PdfExporter.exportFoodDiary(requireContext(), currentMeals, dateStr, firstName)
            }
        }

        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        }

        binding.btnRetry.setOnClickListener { viewModel.loadDiary() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FoodDiaryState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.errorState.visibility  = View.GONE
                }
                is FoodDiaryState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.errorState.visibility  = View.GONE
                    val s = state.summary
                    binding.tvTotalCalories.text =
                        "%.0f / %d ккал".format(s.totalCalories, s.calorieGoal ?: 2000)
                    binding.tvProtein.text = if (s.proteinGoal != null)
                        "Б %.0f / %.0f г".format(s.totalProtein, s.proteinGoal)
                    else "Б %.1f г".format(s.totalProtein)
                    binding.tvFat.text = if (s.fatGoal != null)
                        "Ж %.0f / %.0f г".format(s.totalFat, s.fatGoal)
                    else "Ж %.1f г".format(s.totalFat)
                    binding.tvCarbs.text = if (s.carbGoal != null)
                        "У %.0f / %.0f г".format(s.totalCarbs, s.carbGoal)
                    else "У %.1f г".format(s.totalCarbs)

                    val goal = (s.calorieGoal ?: 2000).toFloat()
                    binding.progressCalories.max      = 100
                    binding.progressCalories.progress = ((s.totalCalories / goal) * 100).toInt().coerceIn(0, 100)

                    currentMeals = s.meals
                    diaryAdapter.submitMeals(s.meals)
                }
                is FoodDiaryState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.text           = state.message
                    binding.errorState.visibility  = View.VISIBLE
                }
            }
        }
    }

    private fun showAddFoodDialog(mealType: String) {
        pendingMealType = mealType
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        searchAdapter = FoodSearchAdapter { product ->
            showWeightDialog(product, mealType)
        }
        dialogBinding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvResults.adapter       = searchAdapter

        dialogBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchFood(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogBinding.btnDone.setOnClickListener {
            viewModel.clearSearch()
            dialog.dismiss()
        }

        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchState.Loading -> dialogBinding.progressSearch.visibility = View.VISIBLE
                is SearchState.Results -> {
                    dialogBinding.progressSearch.visibility = View.GONE
                    searchAdapter.submitList(state.list)
                }
                is SearchState.Idle -> {
                    dialogBinding.progressSearch.visibility = View.GONE
                    searchAdapter.submitList(emptyList())
                }
            }
        }

        dialog.show()
    }

    private fun showWeightDialog(product: FoodProduct, mealType: String) {
        val b = DialogFoodWeightBinding.inflate(layoutInflater)

        b.tvProductName.text     = product.name
        b.tvFoodEmoji.text       = FoodEmojiUtil.getEmoji(product.name)
        b.tvProductCalories.text = "%.0f ккал / 100 г".format(product.caloriesPer100g)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(b.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        fun setWeight(w: String) {
            b.etWeight.setText(w)
            b.etWeight.setSelection(w.length)
        }

        b.chip50.setOnClickListener  { setWeight("50") }
        b.chip100.setOnClickListener { setWeight("100") }
        b.chip150.setOnClickListener { setWeight("150") }
        b.chip200.setOnClickListener { setWeight("200") }
        b.chip300.setOnClickListener { setWeight("300") }

        b.btnCancel.setOnClickListener { dialog.dismiss() }
        b.btnAdd.setOnClickListener {
            val weight = b.etWeight.text.toString().toDoubleOrNull()
            if (weight != null && weight > 0) {
                viewModel.addToMeal(product.productId, weight, mealType)
                dialog.dismiss()
            } else {
                b.tilWeight.error = "Введите корректный вес"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
