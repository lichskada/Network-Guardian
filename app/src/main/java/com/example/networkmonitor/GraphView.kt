package com.example.networkmonitor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class GraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maxPoints = 30
    private val rxData = FloatArray(maxPoints) { 0f }
    private val txData = FloatArray(maxPoints) { 0f }

    private val rxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val txPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4081")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val rxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C00E676")
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#26FFFFFF")
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
        textSize = 26f
    }

    private val rxPath = Path()
    private val rxFillPath = Path()
    private val txPath = Path()

    fun addSample(rxKb: Float, txKb: Float) {
        System.arraycopy(rxData, 1, rxData, 0, maxPoints - 1)
        System.arraycopy(txData, 1, txData, 0, maxPoints - 1)
        rxData[maxPoints - 1] = rxKb
        txData[maxPoints - 1] = txKb
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        var maxSpeed = 10f
        for (i in 0 until maxPoints) {
            maxSpeed = max(maxSpeed, max(rxData[i], txData[i]))
        }

        for (i in 1..3) {
            val y = h - (h / 4 * i)
            canvas.drawLine(0f, y, w, y, gridPaint)
            canvas.drawText(String.format("%.1f KB/s", (maxSpeed / 4) * i), w - 130f, y - 8f, textPaint)
        }

        rxPath.reset()
        rxFillPath.reset()
        txPath.reset()

        val stepX = w / (maxPoints - 1)
        rxPath.moveTo(0f, h - (rxData[0] / maxSpeed * h))
        rxFillPath.moveTo(0f, h)
        rxFillPath.lineTo(0f, h - (rxData[0] / maxSpeed * h))
        txPath.moveTo(0f, h - (txData[0] / maxSpeed * h))

        for (i in 0 until maxPoints - 1) {
            val x1 = i * stepX
            val y1 = h - (rxData[i] / maxSpeed * h)
            val x2 = (i + 1) * stepX
            val y2 = h - (rxData[i + 1] / maxSpeed * h)
            val cx = (x1 + x2) / 2f

            rxPath.cubicTo(cx, y1, cx, y2, x2, y2)
            rxFillPath.cubicTo(cx, y1, cx, y2, x2, y2)

            val txY1 = h - (txData[i] / maxSpeed * h)
            val txY2 = h - (txData[i + 1] / maxSpeed * h)
            txPath.cubicTo(cx, txY1, cx, txY2, x2, txY2)
        }

        rxFillPath.lineTo(w, h)
        rxFillPath.close()

        canvas.drawPath(rxFillPath, rxFillPaint)
        canvas.drawPath(rxPath, rxPaint)
        canvas.drawPath(txPath, txPaint)
    }
}
