package com.appdev.split.Graph

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlinx.parcelize.Parcelize


class CustomBarGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var bars = mutableListOf<BarData>()
    private var selectedPosition = -1
    private var maxValue = 20000f
    private var animationProgress = 1f
    private var showGrid = true
    private var labelCount = 3
    private var gridLineSpacing = 0f
    private var animator: ValueAnimator? = null
    private var initialHighlight: Float? = null
    private var shouldAnimate = true  // New property to control animation
    // Padding values
    private val topPadding = 80f
    private val bottomPadding = 100f  // Increased for more space at bottom
    private val leftPadding = 60f
    private val rightPadding = 40f

    // Label specific padding
    private val labelPadding = 20f
    private val labelBottomMargin = 40f  // Space between labels and bottom edge
    private val yAxisLabelOffset = 8f    // Offset for y-axis labels from grid lines

    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelTopPadding = 25f // Extra padding above the label

    private var yAxisMinimum = 0f
    private var yAxisMaximum = 20000f
    private var yAxisGranularity = 5000f

    private val backgroundPath = Path()
    private val valuePath = Path()
    private val backgroundRect = RectF()
    private val valueRect = RectF()
    private val cornerRadii = FloatArray(8)
    private val textBounds = android.graphics.Rect()
    var currentBarData: List<BarData> = emptyList()
    private var isStateRestored = false

    @Parcelize
    private data class BarDataParcelable(
        val value: Float,
        val label: String,
        val isRaised: Boolean
    ) : Parcelable {
        fun toBarData(): BarData = BarData(
            value = value,
            label = label,
            isRaised = isRaised
        )

        companion object {
            fun fromBarData(barData: BarData): BarDataParcelable =
                BarDataParcelable(
                    value = barData.value,
                    label = barData.label,
                    isRaised = barData.isRaised
                )
        }
    }

    // Then define SavedState
    private class SavedState : BaseSavedState {
        var barDataList: ArrayList<BarDataParcelable> = ArrayList()
        var selectedPosition: Int = -1
        var animationProgress: Float = 1f

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            selectedPosition = parcel.readInt()
            animationProgress = parcel.readFloat()
            parcel.readList(barDataList, BarDataParcelable::class.java.classLoader)
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeInt(selectedPosition)
            parcel.writeFloat(animationProgress)
            parcel.writeList(barDataList)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.selectedPosition = selectedPosition
        savedState.animationProgress = animationProgress
        savedState.barDataList = ArrayList(currentBarData.map { BarDataParcelable.fromBarData(it) })
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        selectedPosition = state.selectedPosition
        animationProgress = state.animationProgress
        currentBarData = state.barDataList.map { it.toBarData() }
        bars = currentBarData.toMutableList()

        isStateRestored = true
        invalidate()
    }


    init {
        for (i in cornerRadii.indices) {
            cornerRadii[i] = 0f
        }

        barPaint.color = Color.parseColor("#333333")
        selectedPaint.color = Color.parseColor("#6750A4")
        backgroundBarPaint.color = Color.parseColor("#EEEEEE")
        labelBackgroundPaint.apply {
            color = Color.parseColor("#6750A4")
            style = Paint.Style.FILL
        }

        textPaint.apply {
            color = Color.parseColor("#333333")
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        textPaint.apply {
            color = Color.parseColor("#333333")
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        gridPaint.apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        labelPaint.apply {
            color = Color.GRAY
            textSize = 30f
            textAlign = Paint.Align.RIGHT
        }
    }

    private fun calculateLeftPadding(): Float {
        // Get the widest possible label in k-format
        val maxLabel = formatNumber(yAxisMaximum)
        val labelWidth = labelPaint.apply {
            textSize = 30f
            textAlign = Paint.Align.RIGHT
        }.measureText(maxLabel)

        // Add generous spacing for readability
        return labelWidth + 24f  // Reduced from 40f since labels are shorter now
    }

    interface OnBarClickListener {
        fun onBarClick(barData: BarData, position: Int)
    }

    private var onBarClickListener: OnBarClickListener? = null



    data class BarData(
        val value: Float,
        val label: String,
        var isRaised: Boolean = false,
        var metadata: Any? = null
    )



    fun setData(data: List<BarData>, animate: Boolean = true) {
        currentBarData = data.toMutableList()
        bars = data.toMutableList()
        shouldAnimate = animate && !isStateRestored

        if (shouldAnimate) {
            animationProgress = 0f
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1000
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    animationProgress = animation.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            animationProgress = 1f
            invalidate()
        }

        isStateRestored = false
    }


    fun highlightValue(x: Float, dataSetIndex: Int) {
        val position = x.toInt()
        if (position in bars.indices) {
            initialHighlight = x
            setSelectedBar(position)
            // Ensure the bar is fully drawn when highlighted
            if (animationProgress < 1f) {
                animationProgress = 1f
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (bars.isEmpty()) return

        val dynamicLeftPadding = calculateLeftPadding()
        val usableWidth = width - dynamicLeftPadding - rightPadding
        val usableHeight = height - topPadding - bottomPadding - labelBottomMargin - 40f
        val barWidth = usableWidth / (bars.size * 2f)
        val spacing = barWidth

        // Update corner radii once
        val cornerRadius = barWidth / 4
        for (i in cornerRadii.indices) {
            cornerRadii[i] = cornerRadius
        }

        if (showGrid) {
            drawGridLinesAndLabels(canvas, usableHeight, dynamicLeftPadding)
        }

        bars.forEachIndexed { index, bar ->
            val left = dynamicLeftPadding + index * (barWidth + spacing) + spacing
            val right = left + barWidth
            val bottom = height - bottomPadding - labelBottomMargin

            // Background bar
            backgroundRect.set(left, topPadding, right, bottom)
            backgroundPath.reset()
            backgroundPath.addRoundRect(backgroundRect, cornerRadii, Path.Direction.CW)
            canvas.drawPath(backgroundPath, backgroundBarPaint)

            val normalizedValue = (bar.value / maxValue).coerceIn(0f, 1f)

            // Calculate the animated height
            val fullHeight = usableHeight * normalizedValue
            val animatedHeight = fullHeight * animationProgress

            // Calculate the animated top position
            val animatedTop = bottom - animatedHeight

            val paint = when {
                index == selectedPosition -> selectedPaint
                else -> barPaint
            }

            if (bar.value > 0) {
                val minHeight = barWidth / 2

                // Use full height immediately if not animating
                val effectiveHeight = if (shouldAnimate) {
                    fullHeight * animationProgress
                } else {
                    fullHeight
                }

                val adjustedTop = if (effectiveHeight < minHeight) bottom - minHeight else bottom - effectiveHeight

                // Only apply top corners during animation when the bar is tall enough
                val animatedCornerRadii = cornerRadii.clone()
                if (animatedHeight < cornerRadius * 2) {
                    // Gradually reveal top corners
                    val topCornerProgress = (animatedHeight / (cornerRadius * 2)).coerceIn(0f, 1f)
                    animatedCornerRadii[0] = cornerRadius * topCornerProgress // top-left
                    animatedCornerRadii[1] = cornerRadius * topCornerProgress
                    animatedCornerRadii[2] = cornerRadius * topCornerProgress // top-right
                    animatedCornerRadii[3] = cornerRadius * topCornerProgress
                }

                valueRect.set(left, adjustedTop, right, bottom)
                valuePath.reset()
                valuePath.addRoundRect(valueRect, animatedCornerRadii, Path.Direction.CW)
                canvas.drawPath(valuePath, paint)

                if (!shouldAnimate || animationProgress > 0.5f || index == selectedPosition) {
                    val valueText = formatNumber(bar.value)
                    textPaint.apply {
                        color = Color.parseColor("#333333")
                        alpha = if (shouldAnimate) {
                            ((animationProgress - 0.5f) * 2 * 255).toInt().coerceIn(0, 255)
                        } else {
                            255
                        }
                    }
                    canvas.drawText(
                        valueText,
                        left + barWidth / 2,
                        adjustedTop - 15,
                        textPaint
                    )
                }
            }

            // Draw x-axis labels
            val labelY = height - (bottomPadding / 2)
            val labelX = left + barWidth / 2

            if (index == selectedPosition) {
                drawSelectedLabel(canvas, bar.label, labelX, labelY)
            } else {
                textPaint.apply {
                    color = Color.parseColor("#333333")
                    alpha = 255
                }
                canvas.drawText(bar.label, labelX, labelY, textPaint)
            }
        }
    }

    private fun drawSelectedLabel(canvas: Canvas, label: String, x: Float, y: Float) {
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        val verticalPadding = labelPadding * 1.5f
        val labelBounds = RectF(
            x - textBounds.width() / 2 - labelPadding,
            y - textBounds.height() - verticalPadding,
            x + textBounds.width() / 2 + labelPadding,
            y + verticalPadding
        )

        canvas.drawRoundRect(
            labelBounds,
            labelPadding * 2,
            labelPadding * 2,
            labelBackgroundPaint
        )

        textPaint.apply {
            color = Color.WHITE
            alpha = 255
        }
        canvas.drawText(label, x, y, textPaint)
    }

    private fun drawGridLinesAndLabels(canvas: Canvas, usableHeight: Float, dynamicLeftPadding: Float) {
        gridLineSpacing = usableHeight / (labelCount - 1)

        for (i in 0 until labelCount) {
            val y = height - bottomPadding - labelBottomMargin - (i * gridLineSpacing)

            // Draw grid line
            canvas.drawLine(
                dynamicLeftPadding,
                y,
                width - rightPadding,
                y,
                gridPaint
            )

            // Draw y-axis label with k format
            val value = yAxisMinimum + (i * yAxisGranularity)
            canvas.drawText(
                formatNumber(value),
                dynamicLeftPadding - 10f,
                y + labelPaint.textSize/2 - yAxisLabelOffset,
                labelPaint
            )
        }
    }

    private fun formatNumber(number: Float): String {
        return when {
            number >= 1000 -> String.format("%.0fk", number / 1000)
            else -> String.format("%.0f", number)
        }
    }

    fun animateY(duration: Int) {
        animator?.cancel()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration.toLong()
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setOnBarClickListener(listener: OnBarClickListener) {
        onBarClickListener = listener
    }

    fun setMaxValue(max: Float) {
        maxValue = max
        yAxisMaximum = max * 1.2f
        yAxisGranularity = max / 2
        invalidate()
    }



    fun setSelectedBar(position: Int) {
        if (position in bars.indices) {
            selectedPosition = position
            invalidate()
            onBarClickListener?.onBarClick(bars[position], position)
        }
    }

    fun getSelectedBarData(): BarData? {
        return if (selectedPosition in bars.indices) {
            bars[selectedPosition]
        } else null
    }

    fun setYAxisConfig(min: Float, max: Float, labelCount: Int) {
        yAxisMinimum = min
        yAxisMaximum = max
        this.labelCount = labelCount
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val barWidth = width / (bars.size * 2f)
                val spacing = barWidth
                val touchX = event.x

                val position = ((touchX - spacing) / (barWidth + spacing)).toInt()
                if (position in bars.indices) {
                    setSelectedBar(position)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}