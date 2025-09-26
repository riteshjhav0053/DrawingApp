package com.example.drawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.toColorInt


class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var mDrawPath: CustomPath? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()

    private var mEraserSize: Float = 0f
    private var isEraser: Boolean = false
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private var lastBrushColor: Int = Color.BLACK


    init{
        setUpDrawing()
    }

    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mDrawPath?.let {
            mDrawPaint!!.strokeWidth = it.brushThickness
            mDrawPaint!!.color = it.color
            canvas.drawPath(it, mDrawPaint!!)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath = CustomPath(color, if (isEraser) mEraserSize else mBrushSize)

                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = if (isEraser) mEraserSize else mBrushSize
                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {

                mDrawPath?.let {
                    canvas?.drawPath(it, mDrawPaint!!)
                    mPaths.add(it) // keep it in history if needed
                }
                mDrawPath = null


                invalidate()
            }
            else -> return false

        }

        return true
    }

    fun setSizeForBrush(newSize:Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        if (!isEraser) {
            mDrawPaint!!.strokeWidth = mBrushSize
        }
    }

    fun setSizeForEraser(newSize: Float) {
        mEraserSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        if (isEraser) {
            mDrawPaint!!.strokeWidth = mEraserSize
        }
    }

    fun setColor(newColor:String){
        disableEraser()
        color = newColor.toColorInt()
        lastBrushColor = color
        mDrawPaint!!.color = color
    }

    fun enableEraser() {
        isEraser = true
        mDrawPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        mDrawPaint!!.strokeWidth = if (mEraserSize > 0) mEraserSize else mBrushSize }

    fun disableEraser() {
        isEraser = false
        mDrawPaint!!.xfermode = null
        color = lastBrushColor
        mDrawPaint!!.color = color
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun isEraserActive(): Boolean {
        return isEraser
    }

    fun getBrushSize(): Float {
        return mBrushSize / resources.displayMetrics.density  // convert back to dp
    }

    fun getEraserSize(): Float {
        return mEraserSize / resources.displayMetrics.density
    }



    internal inner class CustomPath(var color: Int,
        var brushThickness: Float): Path() {

    }
}