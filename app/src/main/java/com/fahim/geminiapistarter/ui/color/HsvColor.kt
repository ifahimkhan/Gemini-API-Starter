package com.fahim.geminiapistarter.ui.color

/**
 * Data class to represent a color in HSV (Hue, Saturation, Value) and Alpha components.
 * Hue is in degrees [0..360].
 * Saturation, Value, and Alpha are in the range [0..1].
 */
data class HsvColor(
    var hue: Float,          // 0..360
    var saturation: Float,   // 0..1
    var value: Float,        // 0..1
    var alpha: Float         // 0..1
) {
    /**
     * Converts this HSV(A) color to an ARGB integer.
     */
    fun toArgbInt(): Int {
        val alphaInt = (alpha * 255).toInt()
        return ColorUtils.hsvToColorInt(hue, saturation, value, alphaInt)
    }

    companion object {
        /**
         * Creates an HsvColor instance from an ARGB integer.
         */
        fun fromArgbInt(argb: Int): HsvColor {
            val hsv = FloatArray(3)
            val alphaFloat = (android.graphics.Color.alpha(argb) / 255.0f)
            android.graphics.Color.colorToHSV(argb, hsv)
            return HsvColor(hsv[0], hsv[1], hsv[2], alphaFloat)
        }
    }
}