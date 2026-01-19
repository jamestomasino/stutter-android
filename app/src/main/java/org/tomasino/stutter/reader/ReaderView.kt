package org.tomasino.stutter.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import org.tomasino.stutter.settings.AppearanceOptions
import kotlin.math.max

class ReaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flankerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val widthCache = mutableMapOf<String, Float>()
    private val layoutCalculator = WordLayoutCalculator()

    private var appearance = AppearanceOptions.DEFAULT
    private var currentText: String = ""
    private var nextText: String? = null
    private var showFlankers: Boolean = false

    init {
        applyAppearance(appearance)
    }

    fun setAppearance(options: AppearanceOptions) {
        appearance = options
        applyAppearance(options)
        widthCache.clear()
        invalidate()
    }

    fun setShowFlankers(show: Boolean) {
        showFlankers = show
        invalidate()
    }

    fun setWord(current: String, next: String?) {
        currentText = current
        nextText = next
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentText.isEmpty()) return

        val parts = layoutCalculator.split(currentText)
        val contentLeft = paddingLeft.toFloat()
        val contentRight = (width - paddingRight).toFloat()
        val contentWidth = max(0f, contentRight - contentLeft)
        val centerX = contentLeft + contentWidth / 2f
        val baseline = paddingTop + (height - paddingTop - paddingBottom) / 2f -
            (centerPaint.ascent() + centerPaint.descent()) / 2f

        val leftWidth = measureText(leftPaint, parts.left)
        val centerWidth = measureText(centerPaint, parts.center)

        val leftStart = centerX - leftWidth - centerWidth / 2f
        canvas.drawText(parts.left, leftStart, baseline, leftPaint)
        canvas.drawText(parts.center, leftStart + leftWidth, baseline, centerPaint)
        canvas.drawText(parts.right, leftStart + leftWidth + centerWidth, baseline, rightPaint)

        if (showFlankers && !nextText.isNullOrEmpty()) {
            val flankerText = nextText ?: ""
            val flankerWidth = measureText(flankerPaint, flankerText)
            val flankerStart = max(leftStart + leftWidth + centerWidth + 24f, centerX + 24f)
            canvas.drawText(flankerText, flankerStart, baseline, flankerPaint)
        }
    }

    private fun applyAppearance(options: AppearanceOptions) {
        val baseSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            options.baseTextSizeSp,
            resources.displayMetrics,
        )
        leftPaint.textSize = baseSizePx
        centerPaint.textSize = baseSizePx * options.centerScale
        rightPaint.textSize = baseSizePx
        flankerPaint.textSize = baseSizePx * 0.75f

        leftPaint.color = options.leftColor
        centerPaint.color = options.centerColor
        rightPaint.color = options.remainderColor
        flankerPaint.color = options.flankerColor

        val typeface = options.fontFamilyName?.let { Typeface.create(it, Typeface.NORMAL) }
        leftPaint.typeface = typeface
        rightPaint.typeface = typeface
        flankerPaint.typeface = typeface
        centerPaint.typeface = if (options.boldCenter) {
            Typeface.create(typeface, Typeface.BOLD)
        } else {
            typeface
        }

        leftPaint.letterSpacing = options.letterSpacingEm
        centerPaint.letterSpacing = options.letterSpacingEm
        rightPaint.letterSpacing = options.letterSpacingEm
        flankerPaint.letterSpacing = options.letterSpacingEm

        val paddingPx = (options.paddingDp * resources.displayMetrics.density).toInt()
        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        setBackgroundColor(options.backgroundColor)
    }

    private fun measureText(paint: Paint, text: String): Float {
        if (text.isEmpty()) return 0f
        val key = "${paint.textSize}:${paint.typeface?.style}:${paint.color}:${paint.letterSpacing}:$text"
        return widthCache.getOrPut(key) { paint.measureText(text) }
    }
}
