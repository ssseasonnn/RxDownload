package zlc.season.rxdownload.kotlin_demo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        basic_download.setOnClickListener {
            startActivity(Intent(this@MainActivity, BasicDownloadActivity::class.java))
        }

        app_market.setOnClickListener {
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
