package com.example.readingpdf

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrixValues = FloatArray(9)
    private val imageMatrixInternal = Matrix()
    private var minScale = 1f
    private var maxScale = 4f
    private var currentScale = 1f

    private var lastTouch = PointF()
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
            val factor = newScale / currentScale
            imageMatrixInternal.postScale(factor, factor, detector.focusX, detector.focusY)
            currentScale = newScale
            imageMatrix = imageMatrixInternal
            parent?.requestDisallowInterceptTouchEvent(currentScale > 1f)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (currentScale > 1f) 1f else 2f
            val factor = target / currentScale
            imageMatrixInternal.postScale(factor, factor, e.x, e.y)
            currentScale = target
            imageMatrix = imageMatrixInternal
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = imageMatrixInternal
    }

    private fun updateCurrentScaleFromMatrix() {
        imageMatrixInternal.getValues(matrixValues)
        currentScale = matrixValues[Matrix.MSCALE_X]
    }

    fun resetZoom() {
        imageMatrixInternal.reset()
        currentScale = 1f
        imageMatrix = imageMatrixInternal
    }

    fun applyExternalMatrix(matrix: Matrix) {
        imageMatrixInternal.set(matrix)
        imageMatrix = imageMatrixInternal
        updateCurrentScaleFromMatrix()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(event.x, event.y)
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(currentScale > 1f)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && event.pointerCount == 1 && currentScale > 1f) {
                    val dx = event.x - lastTouch.x
                    val dy = event.y - lastTouch.y
                    imageMatrixInternal.postTranslate(dx, dy)
                    imageMatrix = imageMatrixInternal
                    lastTouch.set(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
}


