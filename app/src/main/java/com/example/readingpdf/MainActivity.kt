package com.example.readingpdf

import android.app.Activity
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.readingpdf.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pageAdapter: PdfPageAdapter? = null
    private var qualityMultiplier: Float = 1.5f
    private var maxBitmapDim: Int = 3000
    private var currentDocumentId: String? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uri = data?.data
            if (uri != null) {
                val takeFlags = (data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (_: SecurityException) { }
                openPdfFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { pickPdf() }
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_go_to_page -> {
                    showGoToPageDialog()
                    true
                }
                R.id.action_quality -> {
                    showQualityDialog()
                    true
                }
                R.id.action_max_bitmap -> {
                    showBitmapLimitDialog()
                    true
                }
                R.id.action_forget_last -> {
                    Prefs.setLastDocumentUri(this, null)
                    com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.done, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                    true
                }
                R.id.action_reset_settings -> {
                    confirmResetSettings()
                    true
                }
                R.id.action_set_drive_link -> {
                    promptSetDriveLink()
                    true
                }
                R.id.action_fetch_list -> {
                    fetchListFromDrive()
                    true
                }
                R.id.action_open_next -> {
                    openNextFromList()
                    true
                }
                else -> false
            }
        }

        binding.viewPager.setPageTransformer(BookFlipPageTransformer())
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val total = pageAdapter?.itemCount ?: 0
                if (total > 0) {
                    binding.pageIndicator.text = getString(R.string.page, position + 1, total)
                }
            }
        })

        binding.fabPrev.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current > 0) binding.viewPager.setCurrentItem(current - 1, true)
        }
        binding.fabNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            val total = pageAdapter?.itemCount ?: 0
            if (current + 1 < total) binding.viewPager.setCurrentItem(current + 1, true)
        }

        // Persisted prefs
        qualityMultiplier = Prefs.getQuality(this, 1.5f)
        maxBitmapDim = Prefs.getMaxBitmapDim(this, 3000)

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            openPdfFromUri(intent.data!!)
        } else {
            Prefs.getLastDocumentUri(this)?.let { openPdfFromUri(it) }
        }
    }

    private fun confirmResetSettings() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_reset_title)
            .setMessage(R.string.confirm_reset_message)
            .setPositiveButton(R.string.yes) { d, _ ->
                Prefs.resetSettings(this)
                qualityMultiplier = Prefs.getQuality(this, 1.5f)
                maxBitmapDim = Prefs.getMaxBitmapDim(this, 3000)
                // Clear zoom states of current doc if any
                currentDocumentId?.let { Prefs.clearAllZoomForDoc(this, it) }
                pageAdapter?.setQualityAndLimit(qualityMultiplier, maxBitmapDim)
                com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.done, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showGoToPageDialog() {
        val total = pageAdapter?.itemCount ?: return
        val editText = com.google.android.material.textfield.TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.page, 1, total)
        }
        val layout = com.google.android.material.textfield.TextInputLayout(this).apply {
            setPadding(24, 8, 24, 0)
            addView(editText)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Tới trang…")
            .setView(layout)
            .setPositiveButton("OK") { d, _ ->
                val text = editText.text?.toString()?.trim()
                val pageIndex = text?.toIntOrNull()?.minus(1)
                if (pageIndex != null && pageIndex in 0 until total) {
                    binding.viewPager.setCurrentItem(pageIndex, true)
                }
                d.dismiss()
            }
            .setNegativeButton("Hủy") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showQualityDialog() {
        val items = arrayOf(
            getString(R.string.quality_1),
            getString(R.string.quality_1_5),
            getString(R.string.quality_2)
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.render_quality)
            .setItems(items) { d, which ->
                qualityMultiplier = when (which) {
                    0 -> 1.0f
                    1 -> 1.5f
                    else -> 2.0f
                }
                Prefs.setQuality(this, qualityMultiplier)
                pageAdapter?.setQualityAndLimit(qualityMultiplier, maxBitmapDim)
                d.dismiss()
            }
            .show()
    }

    private fun showBitmapLimitDialog() {
        val items = arrayOf(
            getString(R.string.limit_2000),
            getString(R.string.limit_3000),
            getString(R.string.limit_4096)
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bitmap_limit)
            .setItems(items) { d, which ->
                maxBitmapDim = when (which) {
                    0 -> 2000
                    1 -> 3000
                    else -> 4096
                }
                Prefs.setMaxBitmapDim(this, maxBitmapDim)
                pageAdapter?.setQualityAndLimit(qualityMultiplier, maxBitmapDim)
                d.dismiss()
            }
            .show()
    }

    private fun pickPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        openDocument.launch(intent)
    }

    private fun openPdfFromUri(uri: Uri) {
        try {
            // Clean up previous resources and caches before opening new doc
            pageAdapter?.close()
            binding.viewPager.adapter = null
            pdfRenderer?.close()
            fileDescriptor?.close()
            pdfRenderer = null
            fileDescriptor = null

            val tempFile = copyUriToCache(uri)
            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            currentDocumentId = uri.toString()
            Prefs.setLastDocumentUri(this, uri)

            pageAdapter = PdfPageAdapter(this, pdfRenderer!!).apply {
                setQualityAndLimit(qualityMultiplier, maxBitmapDim)
                setZoomPersistenceProvider(
                    save = { pageIndex, values ->
                        currentDocumentId?.let { Prefs.saveZoomMatrix(this@MainActivity, it, pageIndex, values) }
                    },
                    load = { pageIndex ->
                        currentDocumentId?.let { Prefs.loadZoomMatrix(this@MainActivity, it, pageIndex) }
                    }
                )
            }
            binding.viewPager.adapter = pageAdapter

            binding.pageIndicator.text = getString(R.string.page, 1, pageAdapter!!.itemCount)
        } catch (e: Exception) {
            binding.pageIndicator.text = e.message ?: "Error"
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val fileName = "opened.pdf"
        val outFile = File(cacheDir, fileName)
        contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(outFile).use { output ->
                if (input != null) {
                    input.copyTo(output)
                }
            }
        }
        return outFile
    }

    override fun onDestroy() {
        super.onDestroy()
        pageAdapter?.close()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }

    private fun promptSetDriveLink() {
        val editText = com.google.android.material.textfield.TextInputEditText(this).apply {
            setText(Prefs.getDriveLink(this@MainActivity) ?: "")
            hint = getString(R.string.drive_link)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val layout = com.google.android.material.textfield.TextInputLayout(this).apply {
            setPadding(24, 8, 24, 0)
            addView(editText)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.set_drive_link)
            .setView(layout)
            .setPositiveButton(R.string.ok) { d, _ ->
                Prefs.setDriveLink(this, editText.text?.toString()?.trim())
                d.dismiss()
            }
            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun fetchListFromDrive() {
        val link = Prefs.getDriveLink(this)
        if (link.isNullOrBlank()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.drive_link, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }
        com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.downloading, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        scope.launch {
            val text = withContext(Dispatchers.IO) { downloadText(link) }
            if (text.isNullOrBlank()) {
                com.google.android.material.snackbar.Snackbar.make(binding.root, "Download failed", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            } else {
                val items = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                Prefs.setList(this@MainActivity, items)
                com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.done, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun openNextFromList() {
        val list = Prefs.getList(this)
        if (list.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.list_empty, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }
        val idx = Prefs.getListIndex(this)
        val next = list.getOrNull(idx) ?: run {
            com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.list_empty, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val file = withContext(Dispatchers.IO) { downloadFile(next) }
            if (file != null) {
                openPdfFromUri(android.net.Uri.fromFile(file))
                Prefs.setListIndex(this@MainActivity, (idx + 1).coerceAtMost(list.size))
            } else {
                com.google.android.material.snackbar.Snackbar.make(binding.root, "Download failed", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadText(url: String): String? {
        val normalized = maybeConvertGoogleDriveLink(url)
        val conn = URL(normalized).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(url: String): File? {
        val normalized = maybeConvertGoogleDriveLink(url)
        val conn = URL(normalized).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        return try {
            val outFile = File(cacheDir, "dl_${'$'}{System.currentTimeMillis()}.pdf")
            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun maybeConvertGoogleDriveLink(link: String): String {
        val l = link.trim()
        val regex = Regex("https?://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)/?")
        val m = regex.find(l)
        return if (m != null && m.groupValues.size > 1) {
            val id = m.groupValues[1]
            "https://drive.google.com/uc?export=download&id=${'$'}id"
        } else l
    }
}


