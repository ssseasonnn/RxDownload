package zlc.season.rxdownload.demo.utils

import android.graphics.*
import android.graphics.drawable.Drawable
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.manager.*

class ProgressDrawable : Drawable() {
    private var backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var backgroundRectF = RectF()
    private var progressRectF = RectF()

    private var roundCorner = 0

    private var progress = 0L
    private var max = 100L


    init {
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.isDither = true
        backgroundPaint.color = Color.parseColor("#FF009688")

        progressPaint.style = Paint.Style.FILL
        progressPaint.isDither = true
        progressPaint.color = Color.parseColor("#FFD81B60")
    }

    fun setStatus(status: Status) {
        when (status) {
            is Normal,
            is Deleted -> {
                backgroundPaint.color = Color.parseColor("#FF009688")
                this.progress = 0
                this.max = 100
                progressRectF.right = 0f
                invalidateSelf()
            }
            is Started,
            is Downloading,
            is Paused,
            is Completed,
            is Failed -> {
                backgroundPaint.color = Color.GRAY
                this.progress = status.progress.downloadSize
                this.max = status.progress.totalSize
                invalidateSelf()
            }
        }
    }

    fun setProgress(progress: Progress) {
        backgroundPaint.color = Color.GRAY
        this.progress = progress.downloadSize
        this.max = progress.totalSize
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        backgroundRectF = RectF(bounds)
        progressRectF.set(backgroundRectF.left, backgroundRectF.top, 0f, backgroundRectF.bottom)
    }

    override fun draw(canvas: Canvas) {
        val right = backgroundRectF.width() * progress / max
        progressRectF.right = right

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