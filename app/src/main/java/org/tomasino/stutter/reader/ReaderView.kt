package org.tomasino.stutter.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import org.tomasino.stutter.R
import org.tomasino.stutter.settings.AppearanceOptions
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

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
    private var isRtl: Boolean = false

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

    fun setLanguageTag(languageTag: String?) {
        isRtl = resolveIsRtl(languageTag)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentText.isEmpty()) return

        val parts = layoutCalculator.split(currentText)
        val displayParts = if (isRtl) {
            WordParts(parts.right, parts.center, parts.left)
        } else {
            parts
        }
        val contentLeft = paddingLeft.toFloat()
        val contentRight = (width - paddingRight).toFloat()
        val contentWidth = max(0f, contentRight - contentLeft)
        val anchorRatio = if (isRtl) 2f / 3f else 1f / 3f
        val centerX = contentLeft + contentWidth * anchorRatio
        val baselineCenter = paddingTop + (height - paddingTop - paddingBottom) / 2f -
            (centerPaint.ascent() + centerPaint.descent()) / 2f
        val baselineLeft = baselineCenter + baselineDelta(centerPaint, leftPaint)
        val baselineRight = baselineCenter + baselineDelta(centerPaint, rightPaint)
        val baselineFlanker = baselineCenter + baselineDelta(centerPaint, flankerPaint)

        val leftWidth = measureText(leftPaint, displayParts.left)
        val centerWidth = measureText(centerPaint, displayParts.center)

        val leftStart = centerX - leftWidth - centerWidth / 2f
        canvas.drawText(displayParts.left, leftStart, baselineLeft, leftPaint)
        canvas.drawText(displayParts.center, leftStart + leftWidth, baselineCenter, centerPaint)
        canvas.drawText(displayParts.right, leftStart + leftWidth + centerWidth, baselineRight, rightPaint)

        if (showFlankers && !nextText.isNullOrEmpty()) {
            val flankerText = nextText ?: ""
            val flankerWidth = measureText(flankerPaint, flankerText)
            val flankerStart = if (isRtl) {
                min(leftStart - flankerWidth - 24f, centerX - centerWidth / 2f - flankerWidth - 24f)
            } else {
                max(leftStart + leftWidth + centerWidth + 24f, centerX + 24f)
            }
            canvas.drawText(flankerText, flankerStart, baselineFlanker, flankerPaint)
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

        val typeface = resolveTypeface(options.fontFamilyName)
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

    private fun resolveTypeface(name: String?): Typeface? {
        val normalized = name?.trim()?.lowercase(Locale.ROOT)
        return when (normalized) {
            null, "" -> null
            "atkinson-hyperlegible", "atkinson hyperlegible" ->
                loadFont(
                    primary = R.font.atkinson_hyperlegible,
                    fallback = R.font.atkinson_hyperlegible_regular,
                )
            "opendyslexic" ->
                loadFont(
                    primary = R.font.opendyslexic,
                    fallback = R.font.opendyslexic_regular,
                )
            else -> Typeface.create(name, Typeface.NORMAL)
        }
    }

    private fun loadFont(primary: Int, fallback: Int): Typeface? {
        return runCatching { ResourcesCompat.getFont(context, primary) }.getOrNull()
            ?: runCatching { ResourcesCompat.getFont(context, fallback) }.getOrNull()
    }

    private fun baselineDelta(reference: Paint, target: Paint): Float {
        val referenceCenter = (reference.ascent() + reference.descent()) / 2f
        val targetCenter = (target.ascent() + target.descent()) / 2f
        return referenceCenter - targetCenter
    }

    private fun resolveIsRtl(languageTag: String?): Boolean {
        val locale = languageTag
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { Locale.forLanguageTag(it) }
            ?: Locale.getDefault()
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    }
}
