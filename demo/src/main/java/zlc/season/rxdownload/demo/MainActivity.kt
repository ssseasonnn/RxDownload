package zlc.season.rxdownload.demo

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.content_main.*
import zlc.season.rxdownload.demo.basic.DemoActivity
import zlc.season.rxdownload.demo.list.DemoListActivity
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.start

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        setContentView(R.layout.activity_main)

        basic_demo.click {
            start(DemoActivity::class.java)
        }

        demo_list.click {
            start(DemoListActivity::class.java)
        }
    }


    @SuppressLint("CheckResult")
    private fun requestPermission(permission: String) {
        RxPermissions(this)
                .request(permission)
                .subscribe {
                    if (!it) {
                        finish()
                    }
                }
    }

}
