package zlc.season.rxdownload.kotlin_demo

import android.Manifest
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityMainBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ContentMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var mainBinding: ActivityMainBinding
    lateinit var contentBinding: ContentMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        contentBinding = mainBinding.contentMain!!

        setSupportActionBar(mainBinding.toolbar)

        contentBinding.basicDownload.setOnClickListener {
            startActivity(Intent(this@MainActivity, BasicDownloadActivity::class.java))
        }

        contentBinding.appMarket.setOnClickListener {
            startActivity(Intent(this@MainActivity, AppListActivity::class.java))
        }
    }


    private fun requestPermission(permission: String) {
        RxPermissions(this)
                .request(permission)
                .subscribe({
                    if (!it) {
                        finish()
                    }
                })
    }

}
