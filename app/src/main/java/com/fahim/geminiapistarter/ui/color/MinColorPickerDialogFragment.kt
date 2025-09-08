package com.fahim.geminiapistarter.ui.color

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import com.fahim.geminiapistarter.R

interface ColorSelectedListener {
    fun onColorSelected(argb: Int, hex: String)
}

class MinColorPickerDialogFragment : DialogFragment() {

    private lateinit var previewView: View
    private lateinit var alphaRow: ViewGroup
    private lateinit var seekA: SeekBar
    private lateinit var seekR: SeekBar
    private lateinit var seekG: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var hexText: TextView

    private var initialColor: Int = Color.WHITE
    private var showAlpha: Boolean = true
    private var listener: ColorSelectedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_color_picker_min, null)

        previewView = view.findViewById(R.id.previewView)
        alphaRow = view.findViewById(R.id.alphaRow)
        seekA = view.findViewById(R.id.seekA)
        seekR = view.findViewById(R.id.seekR)
        seekG = view.findViewById(R.id.seekG)
        seekB = view.findViewById(R.id.seekB)
        hexText = view.findViewById(R.id.hexText)

        arguments?.let {
            initialColor = it.getInt(ARG_INITIAL_COLOR, Color.WHITE)
            showAlpha = it.getBoolean(ARG_SHOW_ALPHA, true)
        }

        setupSeekBars()
        updatePreview()

        if (!showAlpha) {
            alphaRow.visibility = View.GONE
            seekA.progress = 255
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton(R.string.ok) { _, _ ->
                val currentColor = Color.argb(seekA.progress, seekR.progress, seekG.progress, seekB.progress)
                val currentHex = String.format("#%08X", currentColor)
                listener?.onColorSelected(currentColor, currentHex)
                dismiss()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.ok)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.cancel)
        }

        return dialog
    }

    fun setColorSelectedListener(listener: ColorSelectedListener) {
        this.listener = listener
    }

   override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ColorSelectedListener) {
            listener = context
        } else {
            // Fallback to parentFragment if context is not the listener (e.g. when dialog is shown by another fragment)
            if (parentFragment is ColorSelectedListener) {
                listener = parentFragment as ColorSelectedListener
            } else {
                // If you want to enforce that the activity/fragment must implement it:
                // throw RuntimeException("$context or parent fragment must implement ColorSelectedListener")
                // For this case, we allow it to be set via setColorSelectedListener primarily
            }
        }
    }

    private fun setupSeekBars() {
        seekA.progress = Color.alpha(initialColor)
        seekR.progress = Color.red(initialColor)
        seekG.progress = Color.green(initialColor)
        seekB.progress = Color.blue(initialColor)

        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekA.setOnSeekBarChangeListener(seekBarChangeListener)
        seekR.setOnSeekBarChangeListener(seekBarChangeListener)
        seekG.setOnSeekBarChangeListener(seekBarChangeListener)
        seekB.setOnSeekBarChangeListener(seekBarChangeListener)
    }

    private fun updatePreview() {
        val a = if (showAlpha) seekA.progress else 255
        val r = seekR.progress
        val g = seekG.progress
        val b = seekB.progress
        val currentColor = Color.argb(a, r, g, b)
        val currentHex = String.format("#%08X", currentColor)

        val previewBg = previewView.background
        if (previewBg is GradientDrawable) {
            previewBg.setColor(currentColor)
        } else {
            val newPreviewBg = GradientDrawable()
            newPreviewBg.shape = GradientDrawable.RECTANGLE
            newPreviewBg.setColor(currentColor)
            val strokeDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.bg_color_preview_stroke)
            if (strokeDrawable is GradientDrawable) {
                // Attempt to get stroke details, not straightforward this way
            }
            newPreviewBg.setStroke(2, Color.DKGRAY) // Default stroke
            previewView.background = newPreviewBg
        }

        hexText.text = currentHex
    }

    companion object {
        private const val ARG_INITIAL_COLOR = "initialColor"
        private const val ARG_SHOW_ALPHA = "showAlpha"

        @JvmStatic
        fun newInstance(
            initialColor: Int = Color.WHITE,
            showAlpha: Boolean = true
        ): MinColorPickerDialogFragment {
            val fragment = MinColorPickerDialogFragment()
            val args = Bundle()
            args.putInt(ARG_INITIAL_COLOR, initialColor)
            args.putBoolean(ARG_SHOW_ALPHA, showAlpha)
            fragment.arguments = args
            return fragment
        }
    }
}
