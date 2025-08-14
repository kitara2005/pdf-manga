package com.example.readingpdf

import android.content.Context
import android.net.Uri

object Prefs {
    private const val FILE = "reading_pdf_prefs"
    private const val KEY_QUALITY = "quality_multiplier"
    private const val KEY_MAX_DIM = "max_bitmap_dim"
    private const val KEY_LAST_DOC = "last_doc_uri"
    private const val PREFIX_ZOOM = "zoom:"
    private const val KEY_DRIVE_LINK = "drive_link"
    private const val KEY_LIST = "doc_list"
    private const val KEY_LIST_INDEX = "doc_list_index"

    fun getQuality(context: Context, default: Float = 1.5f): Float {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return sp.getFloat(KEY_QUALITY, default)
    }

    fun setQuality(context: Context, value: Float) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp.edit().putFloat(KEY_QUALITY, value).apply()
    }

    fun getMaxBitmapDim(context: Context, default: Int = 3000): Int {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return sp.getInt(KEY_MAX_DIM, default)
    }

    fun setMaxBitmapDim(context: Context, value: Int) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_MAX_DIM, value).apply()
    }

    fun setLastDocumentUri(context: Context, uri: Uri?) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val v = uri?.toString()
        sp.edit().putString(KEY_LAST_DOC, v).apply()
    }

    fun getLastDocumentUri(context: Context): Uri? {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val s = sp.getString(KEY_LAST_DOC, null) ?: return null
        return try { Uri.parse(s) } catch (_: Throwable) { null }
    }

    fun setDriveLink(context: Context, link: String?) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_DRIVE_LINK, link).apply()
    }

    fun getDriveLink(context: Context): String? {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return sp.getString(KEY_DRIVE_LINK, null)
    }

    fun setList(context: Context, list: List<String>) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LIST, list.joinToString("\n")).apply()
        sp.edit().putInt(KEY_LIST_INDEX, 0).apply()
    }

    fun getList(context: Context): List<String> {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val s = sp.getString(KEY_LIST, null) ?: return emptyList()
        return s.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getListIndex(context: Context): Int {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return sp.getInt(KEY_LIST_INDEX, 0)
    }

    fun setListIndex(context: Context, idx: Int) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp.edit().putInt(KEY_LIST_INDEX, idx).apply()
    }

    private fun zoomKey(doc: String, pageIndex: Int): String = "$PREFIX_ZOOM${doc}:${pageIndex}"

    fun saveZoomMatrix(context: Context, doc: String, pageIndex: Int, matrixValues: FloatArray) {
        if (matrixValues.size < 9) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val payload = matrixValues.joinToString(",")
        sp.edit().putString(zoomKey(doc, pageIndex), payload).apply()
    }

    fun loadZoomMatrix(context: Context, doc: String, pageIndex: Int): FloatArray? {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val payload = sp.getString(zoomKey(doc, pageIndex), null) ?: return null
        val parts = payload.split(',')
        if (parts.size < 9) return null
        return try {
            FloatArray(9) { i -> parts[i].toFloat() }
        } catch (_: Throwable) { null }
    }

    fun clearAllZoomForDoc(context: Context, doc: String) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val all = sp.all
        val keysToRemove = all.keys.filter { it.startsWith("$PREFIX_ZOOM$doc:") }
        if (keysToRemove.isNotEmpty()) {
            val e = sp.edit()
            keysToRemove.forEach { e.remove(it) }
            e.apply()
        }
    }

    fun resetSettings(context: Context) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp.edit()
            .remove(KEY_QUALITY)
            .remove(KEY_MAX_DIM)
            .apply()
    }
}


