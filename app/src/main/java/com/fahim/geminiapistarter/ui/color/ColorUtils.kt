package com.fahim.geminiapistarter.ui.color

import android.graphics.Color
import java.util.Locale

/**
 * Utility functions for color conversions.
 */
object ColorUtils {

    /**
     * Converts HSV components to an ARGB color int.
     * @param h Hue component (0..360)
     * @param s Saturation component (0..1)
     * @param v Value component (0..1)
     * @param a Alpha component (0..255)
     * @return ARGB color integer.
     */
    fun hsvToColorInt(h: Float, s: Float, v: Float, a: Int = 255): Int {
        return Color.HSVToColor(a, floatArrayOf(h, s, v))
    }

    /**
     * Converts an ARGB color integer to a HEX string.
     * @param argb The ARGB color integer.
     * @param includeAlpha Whether to include the alpha component in the HEX string (e.g., #AARRGGBB vs #RRGGBB).
     * @return HEX string representation of the color.
     */
    fun argbToHex(argb: Int, includeAlpha: Boolean = true): String {
        val alpha = Color.alpha(argb)
        val red = Color.red(argb)
        val green = Color.green(argb)
        val blue = Color.blue(argb)

        return if (includeAlpha && alpha < 255) {
            String.format(Locale.ROOT, "#%02X%02X%02X%02X", alpha, red, green, blue)
        } else {
            String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue)
        }
    }

    /**
     * Parses a HEX color string and returns an ARGB integer, or null if parsing fails.
     * Supports formats: #RGB, #ARGB, #RRGGBB, #AARRGGBB.
     * @param hex The HEX color string.
     * @return ARGB color integer or null.
     */
    fun hexToArgbOrNull(hex: String): Int? {
        val cleanHex = if (hex.startsWith("#")) hex.substring(1) else hex
        var alpha: Int = 255
        val red: Int
        val green: Int
        val blue: Int

        try {
            when (cleanHex.length) {
                3 -> { // #RGB
                    red = Integer.parseInt(cleanHex.substring(0, 1) + cleanHex.substring(0, 1), 16)
                    green = Integer.parseInt(cleanHex.substring(1, 2) + cleanHex.substring(1, 2), 16)
                    blue = Integer.parseInt(cleanHex.substring(2, 3) + cleanHex.substring(2, 3), 16)
                }
                4 -> { // #ARGB
                    alpha = Integer.parseInt(cleanHex.substring(0, 1) + cleanHex.substring(0, 1), 16)
                    red = Integer.parseInt(cleanHex.substring(1, 2) + cleanHex.substring(1, 2), 16)
                    green = Integer.parseInt(cleanHex.substring(2, 3) + cleanHex.substring(2, 3), 16)
                    blue = Integer.parseInt(cleanHex.substring(3, 4) + cleanHex.substring(3, 4), 16)
                }
                6 -> { // #RRGGBB
                    red = Integer.parseInt(cleanHex.substring(0, 2), 16)
                    green = Integer.parseInt(cleanHex.substring(2, 4), 16)
                    blue = Integer.parseInt(cleanHex.substring(4, 6), 16)
                }
                8 -> { // #AARRGGBB
                    alpha = Integer.parseInt(cleanHex.substring(0, 2), 16)
                    red = Integer.parseInt(cleanHex.substring(2, 4), 16)
                    green = Integer.parseInt(cleanHex.substring(4, 6), 16)
                    blue = Integer.parseInt(cleanHex.substring(6, 8), 16)
                }
                else -> return null // Invalid length
            }
            return Color.argb(alpha, red, green, blue)
        } catch (e: NumberFormatException) {
            return null // Parsing error
        }
    }
}