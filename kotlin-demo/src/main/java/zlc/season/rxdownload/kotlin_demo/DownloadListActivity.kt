package zlc.season.rxdownload.kotlin_demo

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityDownloadListBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ViewHolderDownloadItemBinding
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.helper.dispose


class DownloadListActivity : AppCompatActivity() {
    lateinit var mainBinding: ActivityDownloadListBinding
    lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_download_list)

        adapter = Adapter()
        mainBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        mainBinding.recyclerView.adapter = adapter

    }


    class Adapter : RecyclerView.Adapter<ViewHolder>() {
        val data = mutableListOf<Mission>()

        init {
            RxDownload.getAllMission()
                    .observeOn(mainThread())
                    .subscribe {
                        data.addAll(it)
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent!!.context)

            val itemBinding: ViewHolderDownloadItemBinding = DataBindingUtil.inflate(inflater,
                    R.layout.view_holder_download_item, parent, false)
            return ViewHolder(itemBinding.root)
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            holder?.setData(data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onViewAttachedToWindow(holder: ViewHolder?) {
            super.onViewAttachedToWindow(holder)
            holder?.onAttach()
        }

        override fun onViewDetachedFromWindow(holder: ViewHolder?) {
            super.onViewDetachedFromWindow(holder)
            holder?.onDetach()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var mission: Mission? = null
        var disposable: Disposable? = null
        var currentStatus: Status? = null

        private val itemBinding: ViewHolderDownloadItemBinding = DataBindingUtil.bind(itemView)

        init {
            itemBinding.action.setOnClickListener {
                when (currentStatus) {
                    is Suspend -> start()
                    is Failed -> start()
                    is Downloading -> stop()
                    is Succeed -> install()
                    is ApkInstallExtension.Installed -> open()
                }
            }
        }

        private fun start() {
            RxDownload.start(mission!!.url).subscribe()
        }

        private fun stop() {
            RxDownload.stop(mission!!.url).subscribe()
        }

        private fun install() {
            RxDownload.extension(mission!!.url, ApkInstallExtension::class.java).subscribe()
        }

        private fun open() {
            //TODO: open app
        }

        fun setData(mission: Mission) {
            this.mission = mission

            mission as CustomMission
            Picasso.with(itemView.context).load(mission.img).into(itemBinding.icon)
        }

        fun onAttach() {
            disposable = RxDownload.create(mission!!.url)
                    .observeOn(mainThread())
                    .subscribe {
                        currentStatus = it
                        setProgress(it)
                        setActionText(it)
                    }
        }

        fun onDetach() {
            dispose(disposable)
        }

        private fun setProgress(it: Status) {
            itemBinding.progressBar.max = it.totalSize.toInt()
            itemBinding.progressBar.progress = it.downloadSize.toInt()

            itemBinding.percent.text = it.percent()
            itemBinding.size.text = it.formatString()
        }

        private fun setActionText(status: Status) {
            val text = when (status) {
                is Suspend -> "开始"
                is Waiting -> "等待中"
                is Downloading -> "暂停"
                is Failed -> "失败"
                is Succeed -> "安装"
                is ApkInstallExtension.Installing -> "安装中"
                is ApkInstallExtension.Installed -> "打开"
                else -> ""
            }
            itemBinding.action.text = text
        }
    }
}