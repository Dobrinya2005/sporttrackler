package com.fitnesstrainer.app.ui.trainer

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentQrScanBinding
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat

class QrScanFragment : Fragment() {

    private var _b: FragmentQrScanBinding? = null
    private val b get() = _b!!
    private val viewModel: TrainerSelectionViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentQrScanBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.barcodeScanner.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        b.barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                b.barcodeScanner.pause()
                val raw = result.text ?: return
                val trainerId = raw.removePrefix("sporttrackler://trainer/").toIntOrNull()
                if (trainerId == null) {
                    Toast.makeText(requireContext(), "Неверный QR-код", Toast.LENGTH_SHORT).show()
                    b.barcodeScanner.resume()
                    return
                }
                viewModel.selectTrainer(trainerId)
            }
        })

        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        b.btnConnectByCode.setOnClickListener { connectByCode() }
        b.etTrainerCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { connectByCode(); true } else false
        }

        viewModel.selectResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SelectResult.Success -> {
                    b.scannerOverlay.scanSuccess = true
                    view.postDelayed({
                        val ctx = context ?: return@postDelayed
                        if (_b == null) return@postDelayed
                        Toast.makeText(ctx, "Тренер привязан!", Toast.LENGTH_SHORT).show()
                        try {
                            findNavController().navigate(
                                R.id.clientDashboardFragment, null,
                                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                            )
                        } catch (_: Exception) {}
                    }, 400)
                }
                is SelectResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSelectResult()
                    b.barcodeScanner.resume()
                }
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        b.barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        b.barcodeScanner.pause()
    }

    private fun connectByCode() {
        val code = b.etTrainerCode.text?.toString()?.trim() ?: ""
        if (code.length < 4) {
            Toast.makeText(requireContext(), "Введите код тренера", Toast.LENGTH_SHORT).show()
            return
        }
        requireContext().getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(b.etTrainerCode.windowToken, 0)
        b.barcodeScanner.pause()
        viewModel.selectTrainerByCode(code)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
