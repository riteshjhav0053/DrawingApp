package com.example.drawingapp

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get


class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var brushBtn: ImageButton

    private var mImageButtonCurrentPaint: ImageButton? = null

    private var mImageButtonFGallery: ImageButton? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            setBackgroundImage(it)  // function to handle image in your drawing view
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        brushBtn = findViewById(R.id.brush_size_btn)
        mImageButtonFGallery = findViewById(R.id.ib_gallery) // replace with your ImageButton id


        mImageButtonFGallery!!.setOnClickListener {
            pickImage.launch("image/*")
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

    private fun setBackgroundImage(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        drawingView.setBackgroundBitmap(bitmap)  // you’ll implement this in DrawingView
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
