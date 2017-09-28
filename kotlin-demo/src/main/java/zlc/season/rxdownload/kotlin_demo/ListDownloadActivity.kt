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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityListDownloadBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ViewHolderDownloadItemBinding
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.helper.dispose


class ListDownloadActivity : AppCompatActivity() {
    lateinit var mainBinding: ActivityListDownloadBinding
    lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_list_download)

        adapter = Adapter()
        mainBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        mainBinding.recyclerView.adapter = adapter

        addData()
    }

    private fun addData() {
        val data = mutableListOf<Item>()
        val images = resources.getStringArray(R.array.image)
        val urls = resources.getStringArray(R.array.url)
        val introduces = resources.getStringArray(R.array.introduce)
        (0 until images.size).mapTo(data) { Item(introduces[it], images[it], urls[it]) }
        adapter.addData(data)
    }

    data class Item(val introduce: String, val img: String, val url: String)


    class Adapter : RecyclerView.Adapter<ViewHolder>() {
        val data = mutableListOf<Item>()

        fun addData(data: MutableList<Item>) {
            this.data.clear()
            this.data.addAll(data)
            notifyDataSetChanged()
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

        override fun onViewDetachedFromWindow(holder: ViewHolder?) {
            super.onViewDetachedFromWindow(holder)
            holder?.dispose()
        }

        override fun onViewRecycled(holder: ViewHolder?) {
            super.onViewRecycled(holder)
            holder?.dispose()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var item: Item? = null
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
            RxDownload.start(item!!.url).subscribe()
        }

        private fun stop() {
            RxDownload.stop(item!!.url).subscribe()
        }

        private fun install() {
            RxDownload.extension(item!!.url, ApkInstallExtension::class.java).subscribe()
        }

        private fun open() {
            //TODO: open app
        }

        fun setData(item: Item) {
            this.item = item

            itemBinding.introduce.text = item.introduce
            Picasso.with(itemView.context).load(item.img).into(itemBinding.icon)

            dispose()
            println("create - ${item.url}")
            disposable = RxDownload.create(item.url)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        println(it)
                        currentStatus = it
                        setActionText(it)
                    })
        }

        fun dispose() {
            dispose(disposable)
        }

        private fun setActionText(status: Status) {
            val text = when (status) {
                is Suspend -> "开始"
                is Waiting -> "等待中"
                is Downloading -> "暂停"
                is Failed -> {
                    println("${item?.url} - failed")
                    "失败"
                }
                is Succeed -> "安装"
                is ApkInstallExtension.Installing -> "安装中"
                is ApkInstallExtension.Installed -> "打开"
                else -> ""
            }
            itemBinding.action.text = text
        }
    }
}