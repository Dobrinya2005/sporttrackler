package com.fitnesstrainer.app.ui.food

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.DialogAddFoodBinding
import com.fitnesstrainer.app.databinding.FragmentFoodDiaryBinding
import java.time.format.DateTimeFormatter

class FoodDiaryFragment : Fragment() {

    private var _binding: FragmentFoodDiaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FoodDiaryViewModel by viewModels()
    private lateinit var diaryAdapter: FoodDiaryAdapter
    private lateinit var searchAdapter: FoodSearchAdapter
    private var pendingMealType = "Breakfast"

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

        binding.btnBack.setOnClickListener  { findNavController().popBackStack() }
        binding.btnPrevDay.setOnClickListener { viewModel.previousDay() }
        binding.btnNextDay.setOnClickListener { viewModel.nextDay() }

        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FoodDiaryState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is FoodDiaryState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val s = state.summary
                    binding.tvTotalCalories.text =
                        "%.0f / %d ккал".format(s.totalCalories, s.calorieGoal ?: 2000)
                    binding.tvProtein.text = "Б %.1f г".format(s.totalProtein)
                    binding.tvFat.text     = "Ж %.1f г".format(s.totalFat)
                    binding.tvCarbs.text   = "У %.1f г".format(s.totalCarbs)

                    val goal = (s.calorieGoal ?: 2000).toFloat()
                    binding.progressCalories.max      = 100
                    binding.progressCalories.progress = ((s.totalCalories / goal) * 100).toInt().coerceIn(0, 100)

                    diaryAdapter.submitMeals(s.meals)
                }
                is FoodDiaryState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddFoodDialog(mealType: String) {
        pendingMealType = mealType
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        searchAdapter = FoodSearchAdapter { product ->
            showWeightDialog(product.productId, product.name, mealType)
        }
        dialogBinding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvResults.adapter       = searchAdapter

        dialogBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also { viewModel.searchFood(query ?: "") }
            override fun onQueryTextChange(newText: String?) = true.also { viewModel.searchFood(newText ?: "") }
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить продукт")
            .setView(dialogBinding.root)
            .setNegativeButton("Отмена") { _, _ -> viewModel.clearSearch() }
            .create()

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

    private fun showWeightDialog(productId: Int, productName: String, mealType: String) {
        val et = android.widget.EditText(requireContext()).apply {
            hint          = "Вес в граммах"
            inputType     = android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(requireContext())
            .setTitle(productName)
            .setView(et)
            .setPositiveButton("Добавить") { _, _ ->
                val weight = et.text.toString().toDoubleOrNull()
                if (weight != null && weight > 0) {
                    viewModel.addToMeal(productId, weight, mealType)
                } else {
                    Toast.makeText(requireContext(), "Введите корректный вес", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
