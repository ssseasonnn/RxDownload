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
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityListDownloadBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ViewHolderDownloadItemBinding


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
            println(data.size)
            return data.size
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var item: Item
        private val itemBinding: ViewHolderDownloadItemBinding = DataBindingUtil.bind(itemView)

        fun setData(item: Item) {
            this.item = item
            itemBinding.introduce.text = item.introduce
            Picasso.with(itemView.context).load(item.img).into(itemBinding.icon)
        }
    }
}