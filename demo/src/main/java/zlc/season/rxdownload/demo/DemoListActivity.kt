package zlc.season.rxdownload.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_demo_list.*
import kotlinx.android.synthetic.main.demo_list_item.*
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.load
import zlc.season.yasha.linear

class DemoListActivity : AppCompatActivity() {

    private val dataSource by lazy { DemoListDataSource() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_list)

        renderList()
    }

    private fun renderList() {
        recycler_view.linear(dataSource) {

            renderItem<DemoListItem> {
                res(R.layout.demo_list_item)

                onBind {
                    tv_name.text = data.name
                    tv_size.text = data.size

                    iv_icon.load(data.icon)

                    btn_action.click {
                        data.action()
                    }
                }

                onAttach {
                    data.subscribe(btn_action)
                    btn_action.text = data.stateStr()
                }

                onDetach {
                    data.dispose()
                }
            }
        }
    }
}
