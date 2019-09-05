package zlc.season.rxdownload.demo.manager

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_demo_list.*
import zlc.season.rxdownload.demo.R
import zlc.season.yasha.linear

class DemoManagerActivity : AppCompatActivity() {
    private val dataSource by lazy { DemoManagerDataSource() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_manager)
        renderList()
    }

    private fun renderList() {
        recycler_view.linear(dataSource) {

            renderItem<DemoManagerItem> {
                res(R.layout.demo_list_item)

                onBind {

                }

                onAttach {
                }

                onDetach {
                }
            }
        }
    }
}
