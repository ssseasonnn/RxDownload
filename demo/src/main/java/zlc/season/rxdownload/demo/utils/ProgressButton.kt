package zlc.season.rxdownload.demo.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import zlc.season.rxdownload4.Progress
import zlc.season.rxdownload4.manager.Status

class ProgressButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Button(context, attrs, defStyleAttr) {

    private val progressDrawable = ProgressDrawable()

    init {
        background(progressDrawable)
    }

    fun setStatus(status: Status) {
        progressDrawable.setStatus(status)
    }

    fun setProgress(progress: Progress) {
        progressDrawable.setProgress(progress)
    }
}