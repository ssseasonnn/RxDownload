package zlc.season.rxdownload.kotlin_demo

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.toolbar)

        binding.contentMain.basicDownload.setOnClickListener {
            startActivity(Intent(this@MainActivity, BasicDownloadActivity::class.java))
        }

    }

}
