package com.example.drawingapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var brushBtn: ImageButton
    var customProgressDialog: Dialog? = null
    private var mImageButtonCurrentPaint: ImageButton? = null

    private var mImageButtonFGallery: ImageButton? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val uri = result.data?.data
                uri?.let {
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                        drawingView.setBackgroundBitmap(bitmap)  // Assumes DrawingView has this method
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var granted = true
            permissions.entries.forEach {
                if (!it.value) granted = false
            }
            if (granted) {
                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            } else {
                Toast.makeText(this, "Permission denied. Can't access gallery or save files.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Dark icons for light background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        drawingView = findViewById(R.id.drawing_view)
        brushBtn = findViewById(R.id.brush_size_btn)
        mImageButtonFGallery = findViewById(R.id.ib_gallery)
        mImageButtonFGallery?.setOnClickListener {
            requestStoragePermission()  // Check and request before launching
        }

        val eraserBtn: ImageButton = findViewById(R.id.eraser_btn)
        val undoBtn: ImageButton = findViewById(R.id.undo_btn)
        val saveBtn: ImageButton = findViewById(R.id.save_btn)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_colorPalette)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.palette_pressed)
        )

        brushBtn.setOnClickListener {
            drawingView.disableEraser()
            showBrushSizeDialog()
        }

        undoBtn.setOnClickListener {
            drawingView.undo()
        }

        saveBtn.setOnClickListener {
            showProgressDialog()
            lifecycleScope.launch {
                val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                val bitmap = getBitmapFromView(flDrawingView)
                saveBitmapFile(bitmap)
            }
        }

        eraserBtn.setOnClickListener {
            drawingView.enableEraser()
            showBrushSizeDialog()
        }
    }

    private fun requestStoragePermission() {
        val permissions = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (deniedPermissions.isNotEmpty()) {
            if (deniedPermissions.any { shouldShowRequestPermissionRationale(it) }) {
                showRationaleDialog("Drawing App", "Drawing App needs storage access to select backgrounds and save drawings.")
            } else {
                requestPermission.launch(deniedPermissions)
            }
        } else {
            // Permissions already granted
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            openGalleryLauncher.launch(pickIntent)
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                requestStoragePermission()  // Request after explanation
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(ContextCompat.getColor(this, R.color.white))
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val cacheDir = externalCacheDir ?: filesDir  // Fallback to internal if null
                    val file = File(cacheDir, "DrawingApp_${System.currentTimeMillis() / 1000}.png")
                    file.parentFile?.mkdirs()  // Ensure directory exists
                    FileOutputStream(file).use { it.write(bytes.toByteArray()) }

                    result = file.absolutePath

                    withContext(Dispatchers.Main) {
                        cancelProgressDialog()
                        Toast.makeText(this@MainActivity, "File saved: $result", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        cancelProgressDialog()
                        Toast.makeText(this@MainActivity, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return result
    }

    private fun showBrushSizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_brush_size, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.brush_size_seekbar)

        if (drawingView.isEraserActive()) {
            seekBar.max = 100
            seekBar.progress = drawingView.getEraserSize().toInt()
        } else {
            seekBar.max = 20
            seekBar.progress = drawingView.getBrushSize().toInt()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // make background transparent and center
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)

        // dim background (gives blur-like effect)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val lp = dialog.window?.attributes
        lp?.dimAmount = 0.6f  // 0 = no dim, 1 = full black
        dialog.window?.attributes = lp

        dialog.show()

        // handle SeekBar value
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress > 0) {
                    if (drawingView.isEraserActive()) {
                        drawingView.setSizeForEraser(progress.toFloat())
                    } else {
                        drawingView.setSizeForBrush(progress.toFloat())
                    }
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palette_pressed)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palette_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
        customProgressDialog?.setCanceledOnTouchOutside(false)
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
}
