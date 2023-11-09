package com.hawky.fr.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 自定义一个View，用于检测人脸
 */
class DetectFaceView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sweepAngle = 0f// 进度
    private var strokeWidth = 0f// 粗细

    init {
        init()
    }

    private fun init() {
        // 画笔--填充
        circlePaint.style = Paint.Style.FILL
        circlePaint.color = Color.WHITE
        strokeWidth = dp2px(10)

        // 画笔--描边
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeCap = Paint.Cap.ROUND
        progressPaint.isAntiAlias = true
        progressPaint.isDither = true
        progressPaint.strokeWidth = strokeWidth
        progressPaint.color = Color.BLUE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制圆圈遮罩
        drawCircleMask(canvas)
    }

    private fun drawCircleMask(canvas: Canvas) {
        canvas.save()
        // 绘制目标图
        canvas.drawRect(Rect(0, 0, width, height), circlePaint)
        // 设置混合模式为擦除
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        // 绘制源图（相当于镂空）
        canvas.drawCircle(
            (width / 2).toFloat(),
            (height / 2).toFloat(),
            (width / 2).toFloat(),
            circlePaint
        )
        val left = 0f
        val top = ((height - width) / 2).toFloat()
        val right = width.toFloat()
        val bottom = ((height + width) / 2).toFloat()
        val oval =
            RectF(left + strokeWidth, top + strokeWidth, right - strokeWidth, bottom - strokeWidth)
        canvas.drawArc(oval, 0f, sweepAngle, false, progressPaint)
        // 清除混合模式
        circlePaint.xfermode = null
        canvas.restore()
    }

    fun setProgress(progress: Int) {
        sweepAngle = progress.toFloat()
        invalidate()
    }

    private fun dp2px(dpValue: Int): Float {
        val metrics = context.resources.displayMetrics
        return metrics.density * dpValue + 0.5f
    }
}