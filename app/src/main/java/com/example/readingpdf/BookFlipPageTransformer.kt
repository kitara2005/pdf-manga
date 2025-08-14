package com.example.readingpdf

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class BookFlipPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.cameraDistance = 20000f

        when {
            position < -1 -> {
                page.alpha = 0f
            }
            position <= 0 -> {
                page.alpha = 1f
                page.pivotX = page.width.toFloat()
                page.pivotY = (page.height / 2).toFloat()
                page.rotationY = 90 * abs(position)
            }
            position <= 1 -> {
                page.alpha = 1f
                page.pivotX = 0f
                page.pivotY = (page.height / 2).toFloat()
                page.rotationY = -90 * abs(position)
            }
            else -> {
                page.alpha = 0f
            }
        }
    }
}


