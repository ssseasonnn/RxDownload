package zlc.season.rxdownload.kotlin_demo

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityMainBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ContentMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var mainBinding: ActivityMainBinding
    lateinit var contentBinding: ContentMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        contentBinding = mainBinding.contentMain!!

        setSupportActionBar(mainBinding.toolbar)

        contentBinding.basicDownload.setOnClickListener {
            startActivity(Intent(this@MainActivity, BasicDownloadActivity::class.java))
        }

        contentBinding.appMarket.setOnClickListener {
            startActivity(Intent(this@MainActivity, ListDownloadActivity::class.java))
        }
    }

}
