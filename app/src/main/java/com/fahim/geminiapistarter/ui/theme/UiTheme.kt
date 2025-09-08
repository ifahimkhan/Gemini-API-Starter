package com.fahim.geminiapistarter.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.fahim.geminiapistarter.R
import com.google.android.material.appbar.MaterialToolbar

object UiTheme {
    private const val PREFS = "ui_prefs"
    private const val KEY_ACCENT = "accent_argb"

    fun loadAccent(context: Context): Int? =
        if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_ACCENT))
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_ACCENT, Color.WHITE)
        else null

    fun saveAccent(context: Context, @ColorInt argb: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCENT, argb).apply()
    }

    fun bestOnColor(@ColorInt bg: Int): Int {
        val r = Color.red(bg) / 255.0
        val g = Color.green(bg) / 255.0
        val b = Color.blue(bg) / 255.0
        val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (lum > 0.5) Color.BLACK else Color.WHITE
    }

    fun darken(@ColorInt c: Int, factor: Float = 0.85f): Int {
        val r = (Color.red(c) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(c) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(c) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(c), r, g, b)
    }

    fun applyAccent(activity: Activity, @ColorInt argb: Int) {
        activity.window.statusBarColor = darken(argb)

        (activity.findViewById<MaterialToolbar>(R.id.topAppBar))?.let { tb ->
            tb.setBackgroundColor(argb)
            tb.setTitleTextColor(bestOnColor(argb))
            tb.navigationIcon?.setTint(bestOnColor(argb))
            tb.overflowIcon?.setTint(bestOnColor(argb)) // Also tint overflow icon if present
            // Tint icons inside the toolbar, like colorPaletteButton and themeToggleButton
            val colorPaletteButton = tb.findViewById<View>(R.id.colorPaletteButton)
            (colorPaletteButton as? android.widget.ImageButton)?.setColorFilter(bestOnColor(argb))
            val themeToggleButton = tb.findViewById<View>(R.id.themeToggleButton)
            (themeToggleButton as? android.widget.ImageButton)?.setColorFilter(bestOnColor(argb))
        }

        val csl = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf(-android.R.attr.state_enabled)),
            intArrayOf(argb, darken(argb, 0.7f))
        )
        activity.findViewById<ViewGroup>(android.R.id.content)?.let { root ->
            tintButtonsRecursively(root, csl, bestOnColor(argb))
        }

        // The following lines for bubble_outgoing and bubble_incoming are illustrative.
        // For RecyclerView items, this logic should be in the adapter's onBindViewHolder.
        activity.findViewById<View?>(R.id.bubble_outgoing)?.let { bubble ->
            val d = AppCompatResources.getDrawable(activity, R.drawable.bg_bubble_outgoing)?.mutate()
            if (d != null) {
                DrawableCompat.setTint(d, argb)
                bubble.background = d
            }
            (bubble as? TextView)?.setTextColor(bestOnColor(argb))
        }

        activity.findViewById<View?>(R.id.bubble_incoming)?.let { bubble ->
            (bubble as? TextView)?.setTextColor(Color.BLACK) // Or bestOnColor(neutral_bg_color)
        }
    }

    private fun tintButtonsRecursively(v: View, tint: ColorStateList, @ColorInt textColor: Int) {
        when (v) {
            is com.google.android.material.button.MaterialButton -> {
                v.backgroundTintList = tint
                v.setTextColor(textColor)
            }
            is Button -> {
                ViewCompat.setBackgroundTintList(v, tint)
                v.setTextColor(textColor)
            }
            // Tint ImageButtons that are not part of the Toolbar (e.g. sendButton, micButton)
            is android.widget.ImageButton -> {
                 // Avoid re-tinting toolbar icons if they are found by this recursive search
                if (v.id != R.id.colorPaletteButton && v.id != R.id.themeToggleButton) {
                    v.imageTintList = tint;
                }
            }
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                tintButtonsRecursively(v.getChildAt(i), tint, textColor)
            }
        }
    }
}
