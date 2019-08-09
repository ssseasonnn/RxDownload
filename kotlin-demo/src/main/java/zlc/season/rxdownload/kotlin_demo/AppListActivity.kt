package zlc.season.rxdownload.kotlin_demo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_app_list.*


class AppListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        toolbar.inflateMenu(R.menu.menu_for_app_list)
        toolbar.setOnMenuItemClickListener {
            startActivity(Intent(this@AppListActivity, DownloadListActivity::class.java))
            return@setOnMenuItemClickListener true
        }

        recycler_view.layoutManager = LinearLayoutManager(this)

        addData()

    }

    private fun addData() {
//        val data = mutableListOf<CustomMission>()
        val images = resources.getStringArray(R.array.image)
        val urls = resources.getStringArray(R.array.url)
        val introduces = resources.getStringArray(R.array.introduce)
//        (0 until images.size).mapTo() {
//            CustomMission(urls[it], introduces[it], images[it])
//        }
//        adapter.addData(data)

    }


}