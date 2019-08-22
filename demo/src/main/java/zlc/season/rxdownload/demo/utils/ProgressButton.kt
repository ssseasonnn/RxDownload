package zlc.season.rxdownload.demo.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.Button

class ProgressButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Button(context, attrs, defStyleAttr) {

    private val progressDrawable = ProgressDrawable()

    init {
        background(progressDrawable)
    }

    fun reset() {
        progressDrawable.reset()
    }

    fun setProgress(downloadSize: Long, totalSize: Long) {
        progressDrawable.setProgress(downloadSize, totalSize)
    }
}