package com.example.drawingapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get


class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var brushBtn: ImageButton

    private var mImageButtonCurrentPaint: ImageButton? = null

    private var mImageButtonFGallery: ImageButton? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground: ImageView = findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
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
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            openGalleryLauncher.launch(intent)
        }


        val eraserBtn: ImageButton = findViewById(R.id.eraser_btn)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_colorPalette)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.palette_pressed)
        )

        brushBtn.setOnClickListener {
            drawingView.disableEraser()
            showBrushSizeDialog()
        }

        eraserBtn.setOnClickListener {
            drawingView.enableEraser()
            showBrushSizeDialog()
        }
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
        if (drawingView.isEraserActive()) {
            seekBar.progress = drawingView.getEraserSize().toInt()
        } else {
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
        if(view !== mImageButtonCurrentPaint){
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

}
