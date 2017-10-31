package zlc.season.rxdownload.kotlin_demo

import android.content.Intent
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
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityAppListBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ViewHolderAppItemBinding
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.extension.ApkOpenExtension
import zlc.season.rxdownload3.helper.dispose


class AppListActivity : AppCompatActivity() {
    lateinit var mainBinding: ActivityAppListBinding
    lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_app_list)

        mainBinding.toolbar.inflateMenu(R.menu.menu_for_app_list)
        mainBinding.toolbar.setOnMenuItemClickListener {
            startActivity(Intent(this@AppListActivity, DownloadListActivity::class.java))
            return@setOnMenuItemClickListener true
        }

        adapter = Adapter()
        mainBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        mainBinding.recyclerView.adapter = adapter

        addData()

    }

    private fun addData() {
        val data = mutableListOf<CustomMission>()
        val images = resources.getStringArray(R.array.image)
        val urls = resources.getStringArray(R.array.url)
        val introduces = resources.getStringArray(R.array.introduce)
        (0 until images.size).mapTo(data) { CustomMission(urls[it], introduces[it], images[it]) }
        adapter.addData(data)

        createAllMissionOnStart(data)
    }

    private fun createAllMissionOnStart(data: MutableList<CustomMission>) {
        RxDownload.createAll(data).subscribe()
    }


    class Adapter : RecyclerView.Adapter<ViewHolder>() {
        val data = mutableListOf<CustomMission>()

        fun addData(data: MutableList<CustomMission>) {
            this.data.clear()
            this.data.addAll(data)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent!!.context)

            val itemBinding: ViewHolderAppItemBinding = DataBindingUtil.inflate(inflater,
                    R.layout.view_holder_app_item, parent, false)
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
        private var customMission: CustomMission? = null
        var disposable: Disposable? = null
        var currentStatus: Status? = null

        private val itemBinding: ViewHolderAppItemBinding = DataBindingUtil.bind(itemView)

        init {
            itemBinding.action.setOnClickListener {
                when (currentStatus) {
                    is Normal -> start()
                    is Suspend -> start()
                    is Failed -> start()
                    is Deleted -> start()
                    is Downloading -> stop()
                    is Succeed -> install()
                    is ApkInstallExtension.Installed -> open()
                }
            }
        }

        private fun start() {
            RxDownload.start(customMission!!).subscribe()
        }

        private fun stop() {
            RxDownload.stop(customMission!!).subscribe()
        }

        private fun install() {
            RxDownload.extension(customMission!!, ApkInstallExtension::class.java).subscribe()
        }

        private fun open() {
            RxDownload.extension(customMission!!, ApkOpenExtension::class.java).subscribe()
        }

        fun setData(customMission: CustomMission) {
            this.customMission = customMission

            itemBinding.introduce.text = customMission.introduce
            Picasso.with(itemView.context).load(customMission.img).into(itemBinding.icon)

        }

        fun onAttach() {
            disposable = RxDownload.create(customMission!!)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        currentStatus = it
                        setActionText(it)
                    })
        }

        fun onDetach() {
            dispose(disposable)
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
                is Deleted -> "开始"
                else -> ""
            }
            itemBinding.action.text = text
        }
    }
}