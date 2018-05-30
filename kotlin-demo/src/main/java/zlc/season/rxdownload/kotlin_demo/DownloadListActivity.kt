package zlc.season.rxdownload.kotlin_demo

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
import kotlinx.android.synthetic.main.activity_download_list.*
import kotlinx.android.synthetic.main.view_holder_download_item.view.*
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.extension.ApkOpenExtension
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.helper.loge


class DownloadListActivity : AppCompatActivity() {
    lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_list)

        adapter = Adapter()
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter

        startAll.setOnClickListener {
            RxDownload.startAll().subscribe()
        }

        stopAll.setOnClickListener {
            RxDownload.stopAll().subscribe()
        }

        deleteAll.setOnClickListener {
            RxDownload.deleteAll().subscribe {
                loadData()
            }
        }

        loadData()
    }

    private fun loadData() {
        RxDownload.getAllMission()
                .observeOn(mainThread())
                .subscribe {
                    adapter.addData(it)
                }
    }


    class Adapter : RecyclerView.Adapter<ViewHolder>() {
        val data = mutableListOf<Mission>()

        fun addData(data: List<Mission>) {
            this.data.clear()
            this.data.addAll(data)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent!!.context)

            return ViewHolder(inflater.inflate(R.layout.view_holder_download_item, parent, false))
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
        private var disposable: Disposable? = null
        private var currentStatus: Status? = null


        init {
            itemView.action.setOnClickListener {
                when (currentStatus) {
                    is Normal -> start()
                    is Suspend -> start()
                    is Failed -> start()
                    is Downloading -> stop()
                    is Succeed -> install()
                    is ApkInstallExtension.Installed -> open()
                }
            }
        }

        private fun start() {
            RxDownload.start(mission!!.url).subscribe({}, { println(it) })
        }

        private fun stop() {
            RxDownload.stop(mission!!.url).subscribe()
        }

        private fun install() {
            RxDownload.extension(mission!!.url, ApkInstallExtension::class.java).subscribe()
        }

        private fun open() {
            RxDownload.extension(mission!!.url, ApkOpenExtension::class.java).subscribe()
        }

        fun setData(mission: Mission) {
            this.mission = mission

            mission as CustomMission
            Picasso.with(itemView.context).load(mission.img).into(itemView.icon)
        }

        fun onAttach() {
            disposable = RxDownload.create(mission!!.url)
                    .observeOn(mainThread())
                    .subscribe {
                        if (currentStatus is Failed) {
                            loge("Failed", (currentStatus as Failed).throwable)
                        }
                        currentStatus = it
                        setProgress(it)
                        setActionText(it)
                    }
        }

        fun onDetach() {
            dispose(disposable)
        }

        private fun setProgress(it: Status) {
            itemView.progressBar.max = it.totalSize.toInt()
            itemView.progressBar.progress = it.downloadSize.toInt()

            itemView.percent.text = it.percent()
            itemView.size.text = it.formatString()
        }

        private fun setActionText(status: Status) {
            val text = when (status) {
                is Normal -> "开始"
                is Suspend -> "已暂停"
                is Waiting -> "等待中"
                is Downloading -> "暂停"
                is Failed -> "失败"
                is Succeed -> "安装"
                is ApkInstallExtension.Installing -> "安装中"
                is ApkInstallExtension.Installed -> "打开"
                else -> ""
            }
            itemView.action.text = text
        }
    }
}