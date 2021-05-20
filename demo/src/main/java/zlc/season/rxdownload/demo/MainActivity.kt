package zlc.season.rxdownload.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.content_main.*
import zlc.season.rxdownload.demo.basic.DemoActivity
import zlc.season.rxdownload.demo.list.DemoListActivity
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.start

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        basic_demo.click {
            start(DemoActivity::class.java)
        }

        demo_list.click {
            start(DemoListActivity::class.java)
        }
    }

}
