package com.example.readingpdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.util.LruCache
import androidx.appcompat.widget.AppCompatImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PdfPageAdapter(
    private val context: Context,
    private val pdfRenderer: PdfRenderer
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ZoomImageView = itemView as ZoomImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val imageView = ZoomImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            adjustViewBounds = true
            scaleType = AppCompatImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }
        return PageViewHolder(imageView)
    }

    override fun getItemCount(): Int = pdfRenderer.pageCount

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        renderInto(holder, position)
        restoreZoomIfAny(holder.imageView, position)
    }

    private val cache: LruCache<Int, Bitmap> by lazy {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = maxMemoryKb / 8 // Use 1/8 of available memory for cache
        object : LruCache<Int, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: Int, value: Bitmap): Int {
                return value.byteCount / 1024
            }
            override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
                if (evicted && !oldValue.isRecycled) {
                    oldValue.recycle()
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val renderMutex = Mutex()

    private fun renderInto(holder: PageViewHolder, position: Int) {
        val cached = cache.get(position)
        if (cached != null && !cached.isRecycled) {
            holder.imageView.setImageBitmap(cached)
            preloadNeighbors(position)
            return
        }

        holder.imageView.setImageBitmap(null)
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                renderMutex.withLock {
                    val page = pdfRenderer.openPage(position)
                    val pdfW = page.width
                    val pdfH = page.height
                    val viewW = holder.imageView.width
                    val viewH = holder.imageView.height
                    val (bw, bh) = calculateTargetBitmapSize(pdfW, pdfH, viewW, viewH)
                    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                    val destRect = Rect(0, 0, bw, bh)
                    page.render(bmp, destRect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                }
            }

            cache.put(position, bitmap)
            if (holder.bindingAdapterPosition == position) {
                holder.imageView.setImageBitmap(bitmap)
            }
            preloadNeighbors(position)
        }
    }

    private fun preloadNeighbors(position: Int) {
        val neighbors = listOf(position - 1, position + 1).filter { it in 0 until itemCount }
        for (p in neighbors) {
            if (cache.get(p) != null) continue
            scope.launch(Dispatchers.Default) {
                try {
                    val bmp = renderMutex.withLock {
                        val page = pdfRenderer.openPage(p)
                        val dm = context.resources.displayMetrics
                        val (bw, bh) = calculateTargetBitmapSize(page.width, page.height, dm.widthPixels, dm.heightPixels)
                        val b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                        page.render(b, Rect(0, 0, bw, bh), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        b
                    }
                    withContext(Dispatchers.Main) {
                        if (cache.get(p) == null) cache.put(p, bmp) else bmp.recycle()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    // Zoom state per page
    private val pageScaleMap = HashMap<Int, Float>()
    private val pageMatrixMap = HashMap<Int, android.graphics.Matrix>()
    private var persistSave: ((Int, FloatArray) -> Unit)? = null
    private var persistLoad: ((Int) -> FloatArray?)? = null

    fun setZoomPersistenceProvider(
        save: (pageIndex: Int, matrixValues: FloatArray) -> Unit,
        load: (pageIndex: Int) -> FloatArray?
    ) {
        persistSave = save
        persistLoad = load
    }

    private fun restoreZoomIfAny(view: ZoomImageView, position: Int) {
        var matrix = pageMatrixMap[position]
        if (matrix == null) {
            val loaded = persistLoad?.invoke(position)
            if (loaded != null && loaded.size >= 9) {
                val m = android.graphics.Matrix()
                m.setValues(loaded)
                matrix = m
                pageMatrixMap[position] = m
                // also store approximate scale
                val currentScale = loaded[android.graphics.Matrix.MSCALE_X]
                pageScaleMap[position] = currentScale
            }
        }

        if (matrix != null) {
            view.applyExternalMatrix(matrix)
        } else {
            view.resetZoom()
        }
        view.setOnTouchListener { v, event ->
            val handled = view.onTouchEvent(event)
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP || event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) {
                // store matrix and current scale
                val current = android.graphics.Matrix(view.imageMatrix)
                pageMatrixMap[position] = current
                // approximate scale from matrix
                val values = FloatArray(9)
                current.getValues(values)
                val currentScale = values[android.graphics.Matrix.MSCALE_X]
                pageScaleMap[position] = currentScale
                // persist
                persistSave?.invoke(position, values)
            }
            handled
        }
    }

    private fun calculateTargetBitmapSize(pdfW: Int, pdfH: Int, viewW: Int, viewH: Int): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        val targetW = if (viewW > 0) viewW else dm.widthPixels
        val targetH = if (viewH > 0) viewH else dm.heightPixels

        val scale = minOf(targetW.toFloat() / pdfW, targetH.toFloat() / pdfH)
        var bw = (pdfW * scale * qualityMultiplier).toInt().coerceAtLeast(1)
        var bh = (pdfH * scale * qualityMultiplier).toInt().coerceAtLeast(1)

        val maxDim = maxBitmapDim
        if (bw > maxDim || bh > maxDim) {
            val reduce = minOf(maxDim.toFloat() / bw, maxDim.toFloat() / bh)
            bw = (bw * reduce).toInt().coerceAtLeast(1)
            bh = (bh * reduce).toInt().coerceAtLeast(1)
        }
        return Pair(bw, bh)
    }

    private var qualityMultiplier: Float = 1.5f
    private var maxBitmapDim: Int = 3000

    fun setQualityAndLimit(multiplier: Float, maxDim: Int) {
        qualityMultiplier = multiplier
        maxBitmapDim = maxDim
        notifyDataSetChanged()
    }

    fun close() {
        scope.cancel()
        for (i in 0 until itemCount) {
            cache.get(i)?.let { if (!it.isRecycled) it.recycle() }
        }
        cache.evictAll()
    }
}


