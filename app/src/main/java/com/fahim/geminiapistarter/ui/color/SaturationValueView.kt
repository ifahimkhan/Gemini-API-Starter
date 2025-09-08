package com.fahim.geminiapistarter.ui.color

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A custom View for selecting Saturation and Value of a color, given a Hue.
 * Displays a 2D gradient and a draggable selector.
 */
class SaturationValueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnSaturationValueChangeListener {
        fun onSaturationValueChanged(saturation: Float, value: Float)
    }

    private var listener: OnSaturationValueChangeListener? = null

    private val svPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    private var currentHue: Float = 0f // 0-360
    var saturation: Float = 1f // 0-1
        private set
    var value: Float = 1f // 0-1
        private set

    private val thumbRadiusDp = 10f
    private val thumbStrokeWidthDp = 2f
    private var thumbRadiusPx: Float = 0f
    private var thumbActualRadiusPx: Float = 0f // Radius including stroke for touch calculation

    private var saturationShader: Shader? = null
    private var valueShader: Shader? = null
    private var composedShader: Shader? = null

    init {
        val density = context.resources.displayMetrics.density
        thumbRadiusPx = thumbRadiusDp * density
        val thumbStrokePx = thumbStrokeWidthDp * density
        thumbActualRadiusPx = thumbRadiusPx + thumbStrokePx / 2

        thumbPaint.style = Paint.Style.STROKE
        thumbPaint.strokeWidth = thumbStrokePx
        // Thumb color will be set in onDraw based on current selection
    }

    fun setOnSaturationValueChangeListener(listener: OnSaturationValueChangeListener) {
        this.listener = listener
    }

    /**
     * Sets the current Hue and updates the Saturation/Value gradient.
     * @param hue Hue value (0-360).
     */
    fun setHue(hue: Float) {
        if (currentHue != hue) {
            currentHue = hue
            updateShaders()
            invalidate()
        }
    }

    /**
     * Sets the selected color using HsvColor object.
     * This will update hue, saturation, and value.
     */
    fun setColor(hsvColor: HsvColor) {
        var needsInvalidate = false
        if (currentHue != hsvColor.hue) {
            currentHue = hsvColor.hue
            updateShaders() // Shader depends on hue
            needsInvalidate = true
        }
        val newSat = hsvColor.saturation.coerceIn(0f, 1f)
        val newVal = hsvColor.value.coerceIn(0f, 1f)

        if (saturation != newSat || value != newVal) {
            saturation = newSat
            value = newVal
            needsInvalidate = true
        }
        if (needsInvalidate) {
            invalidate()
        }
    }


    private fun updateShaders() {
        if (viewWidth <= 0 || viewHeight <= 0) return

        val fullHueColor = ColorUtils.hsvToColorInt(currentHue, 1f, 1f)

        saturationShader = LinearGradient(
            0f, 0f, viewWidth.toFloat(), 0f,
            Color.WHITE, fullHueColor,
            Shader.TileMode.CLAMP
        )

        valueShader = LinearGradient(
            0f, 0f, 0f, viewHeight.toFloat(),
            Color.TRANSPARENT, Color.BLACK,
            Shader.TileMode.CLAMP
        )

        composedShader = ComposeShader(valueShader!!, saturationShader!!, PorterDuff.Mode.MULTIPLY)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        updateShaders()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw Saturation/Value gradient
        if (composedShader != null) {
            svPaint.shader = composedShader
            canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), svPaint)
        }

        // Calculate thumb position
        val thumbX = saturation * viewWidth
        val thumbY = (1f - value) * viewHeight // Value is 0 at bottom, 1 at top

        // Draw selector thumb
        // Choose thumb color based on contrast with the selected color
        val selectedColorInt = ColorUtils.hsvToColorInt(currentHue, saturation, value)
        // If color is dark, use white thumb, else use black.
        // A simple brightness check (sum of RGB components)
        val brightness = Color.red(selectedColorInt) + Color.green(selectedColorInt) + Color.blue(selectedColorInt)
        thumbPaint.color = if (brightness < (255 * 3 / 2)) Color.WHITE else Color.BLACK

        canvas.drawCircle(thumbX, thumbY, thumbRadiusPx, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.coerceIn(0f, viewWidth.toFloat())
        val y = event.y.coerceIn(0f, viewHeight.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                saturation = x / viewWidth
                value = 1f - (y / viewHeight) // Value is 0 at bottom, 1 at top

                listener?.onSaturationValueChanged(saturation, value)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val desiredWidth = 200 * resources.displayMetrics.density.roundToInt() // Example default size
        val desiredHeight = desiredWidth // Aim for square

        val width: Int
        val height: Int

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = min(desiredWidth, widthSize)
        } else {
            width = desiredWidth
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = min(desiredHeight, heightSize)
        } else {
            height = desiredHeight
        }
        // For simplicity, let's try to make it square if possible, based on width
        // If one dimension is exact, and the other isn't, try to match the exact one.
        val finalSize = if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
            width
        } else if (heightMode == MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {
            height
        } else {
            // If both are wrap_content or at_most, use the smaller of the two desired/limited sizes
            // or if both exact, they are what they are.
            // This example aims for width to define height for a square, if height allows.
            min(width, height) // A simple square approach based on the resolved width or height.
                               // A better approach would be to respect both constraints and
                               // calculate the largest possible square or use a specific aspect ratio.
                               // For now, this aims for a square.
        }
         // Make it square based on the smaller of the two calculated dimensions
        val squareSize = min(width, height)
        setMeasuredDimension(squareSize, squareSize)
    }
}