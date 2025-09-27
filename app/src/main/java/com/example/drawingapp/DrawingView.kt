package com.example.drawingapp

import android.R.attr.path
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
import android.graphics.Rect
import androidx.core.graphics.toColorInt


class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()

    private var mEraserSize: Float = 0f
    private var isEraser: Boolean = false
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()
    private var lastBrushColor: Int = Color.BLACK
    private var backgroundBitmap: Bitmap? = null



    init{
        setUpDrawing()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun undo(){
        if(mPaths.isNotEmpty()){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            redrawBitmapFromPaths()
            invalidate()
        }
    }

    fun setBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()  // Redraw to apply the new background
    }


    private fun redrawBitmapFromPaths() {
        if (mCanvasBitmap == null) return
        // Clear the bitmap
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        // Replay all paths on the bitmap
        for (path in mPaths) {
            val paint = Paint()
            paint.strokeWidth = path.brushThickness
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            if (path.isEraser) {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                paint.xfermode = null
                paint.color = path.color
            }
            canvas?.drawPath(path, paint)
        }
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
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        backgroundBitmap?.let {
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(it, null, dstRect, mCanvasPaint)
        }

        mCanvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, mCanvasPaint) }

        mDrawPath?.let {
            val paint = Paint(mDrawPaint!!)
            paint.strokeWidth = it.brushThickness
            if(it.isEraser){
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                paint.xfermode = null
                paint.color = it.color
            }
            canvas.drawPath(it, paint)
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath = CustomPath(
                    color = if (isEraser) Color.TRANSPARENT else color,
                    if (isEraser) mEraserSize else mBrushSize, isEraser
                )
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
                mDrawPath?.let { path ->
                    val paint = Paint(mDrawPaint!!)
                    paint.strokeWidth = path.brushThickness
                    paint.style = Paint.Style.STROKE
                    paint.strokeJoin = Paint.Join.ROUND
                    paint.strokeCap = Paint.Cap.ROUND
                    if (path.isEraser) {
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    } else {
                        paint.xfermode = null
                        paint.color = path.color
                    }
                    canvas?.drawPath(path, paint)
                    mPaths.add(path)
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
        if (!isEraser) {
            lastBrushColor = color // store current brush color
        }
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

    internal inner class CustomPath(
        var color: Int,
        var brushThickness: Float,
        var isEraser: Boolean = false
    ) : Path()
}