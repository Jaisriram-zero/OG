package com.example.supernoteclassic

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.content.Context
import android.view.ViewGroup
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var drawingView: DrawingView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(55, 67, 75)
        window.navigationBarColor = Color.BLACK

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(238, 235, 226))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(55, 67, 75))
        }
        val title = TextView(this).apply {
            text = "SuperNote Classic"
            textSize = 20f
            setTextColor(Color.WHITE)
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val save = Button(this).apply {
            text = "Save"
            setOnClickListener { saveDrawing() }
        }
        header.addView(save)
        page.addView(header)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(4, 4, 4, 4)
            setBackgroundColor(Color.rgb(220, 216, 205))
        }
        fun tool(label: String, action: () -> Unit) {
            toolbar.addView(Button(this).apply {
                text = label
                textSize = 12f
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, 48, 1f))
        }

        drawingView = DrawingView(this)
        tool("Pen") { drawingView.setEraser(false); status.text = "Pen selected" }
        tool("Blue") { drawingView.setColor(Color.rgb(30, 80, 180)); drawingView.setEraser(false); status.text = "Blue pen" }
        tool("Red") { drawingView.setColor(Color.RED); drawingView.setEraser(false); status.text = "Red pen" }
        tool("Erase") { drawingView.setEraser(true); status.text = "Eraser selected" }
        tool("Clear") { drawingView.clear(); status.text = "Page cleared" }
        page.addView(toolbar)

        status = TextView(this).apply {
            text = "Notebook • Page 1 • Draw with your finger"
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(12, 6, 12, 6)
        }
        page.addView(status)

        drawingView.setBackgroundColor(Color.WHITE)
        page.addView(drawingView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(page)
    }

    private fun saveDrawing() {
        try {
            val bitmap = drawingView.exportBitmap()
            val file = File(filesDir, "supernote-page.png")
            FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(this, "Saved inside app storage: supernote-page.png", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not save page", Toast.LENGTH_SHORT).show()
        }
    }
}

class DrawingView(context: Context) : View(context) {
    private data class Stroke(val path: Path, val paint: Paint)
    private val strokes = mutableListOf<Stroke>()
    private var currentPath: Path? = null
    private var currentPaint = makePaint(Color.BLACK, 5f)
    private var eraser = false

    private fun makePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = width
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun setColor(color: Int) {
        eraser = false
        currentPaint = makePaint(color, 5f)
    }

    fun setEraser(enabled: Boolean) {
        eraser = enabled
        currentPaint = if (enabled) makePaint(Color.WHITE, 28f) else makePaint(Color.BLACK, 5f)
    }

    fun clear() {
        strokes.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        for (stroke in strokes) canvas.drawPath(stroke.path, stroke.paint)
        currentPath?.let { canvas.drawPath(it, currentPaint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply { moveTo(event.x, event.y) }
                strokes.add(Stroke(currentPath!!, Paint(currentPaint)))
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                currentPath?.lineTo(event.x, event.y)
                currentPath = null
                invalidate()
                return true
            }
        }
        return true
    }

    fun exportBitmap(): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(
            maxOf(width, 1), maxOf(height, 1), android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        for (stroke in strokes) canvas.drawPath(stroke.path, stroke.paint)
        return bitmap
    }
}
