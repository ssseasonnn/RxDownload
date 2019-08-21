package zlc.season.rxdownload.demo.utils

import android.graphics.*
import android.graphics.drawable.Drawable

class ProgressDrawable : Drawable() {
    private var backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var backgroundRectF = RectF()
    private var progressRectF = RectF()

    private var roundCorner = 0

    private var progress = 0L


    init {
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.isDither = true
        backgroundPaint.color = Color.parseColor("#008577")

        progressPaint.style = Paint.Style.FILL
        progressPaint.isDither = true
        progressPaint.color = Color.parseColor("#D81B60")
    }

    fun setProgress(progress: Long, max: Long) {
        backgroundPaint.color = Color.GRAY

        this.progress = progress
        val right = backgroundRectF.width() * progress / max
        progressRectF.right = right
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        backgroundRectF = RectF(bounds)
        progressRectF.set(backgroundRectF.left, backgroundRectF.top, 0f, backgroundRectF.bottom)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(backgroundRectF, roundCorner.toFloat(), roundCorner.toFloat(), backgroundPaint)
        canvas.drawRoundRect(progressRectF, roundCorner.toFloat(), roundCorner.toFloat(), progressPaint)
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
        progressPaint.alpha = alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
        progressPaint.colorFilter = colorFilter
    }
}