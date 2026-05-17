package com.fitnesstrainer.app.ui.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.fitnesstrainer.app.databinding.DialogTwoFactorBinding

class TwoFactorDialog : DialogFragment() {

    private var _b: DialogTwoFactorBinding? = null
    private val b get() = _b!!

    var onVerify: ((String) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = DialogTwoFactorBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cells = listOf(b.otp1, b.otp2, b.otp3, b.otp4)

        // Auto-advance and auto-backspace between cells
        cells.forEachIndexed { i, cell ->
            cell.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty()) {
                        cells.getOrNull(i + 1)?.requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
            cell.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_DEL &&
                    cell.text.isNullOrEmpty()) {
                    cells.getOrNull(i - 1)?.apply { requestFocus(); text.clear() }
                    true
                } else false
            }
        }

        b.btnVerify.setOnClickListener {
            val code = cells.joinToString("") { it.text.toString() }
            if (code.length == 4) {
                onVerify?.invoke(code)
                dismiss()
            } else {
                cells.forEach { if (it.text.isNullOrEmpty()) { it.requestFocus(); return@setOnClickListener } }
            }
        }

        b.btnClear.setOnClickListener {
            cells.forEach { it.text.clear() }
            cells.first().requestFocus()
        }

        b.btnClose.setOnClickListener { dismiss() }

        cells.first().requestFocus()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    companion object {
        fun newInstance(onVerify: (String) -> Unit) =
            TwoFactorDialog().apply { this.onVerify = onVerify }
    }
}
