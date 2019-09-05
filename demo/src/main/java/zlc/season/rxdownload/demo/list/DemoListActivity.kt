package zlc.season.rxdownload.demo.list

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_demo_list.*
import kotlinx.android.synthetic.main.demo_list_item.*
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.manager.DemoManagerActivity
import zlc.season.rxdownload.demo.utils.click
import zlc.season.rxdownload.demo.utils.load
import zlc.season.rxdownload.demo.utils.start
import zlc.season.yasha.linear

class DemoListActivity : AppCompatActivity() {

    private val dataSource by lazy { DemoListDataSource() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_list)
        setSupportActionBar(toolbar)

        renderList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.task_manager, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_download_manager -> {
                start(DemoManagerActivity::class.java)
            }
        }
        return super.onOptionsItemSelected(item)
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
                        data.action(containerView.context)
                    }
                }

                onAttach {
                    data.subscribe(btn_action, containerView.context)
                }

                onDetach {
                    data.dispose()
                }
            }
        }
    }
}
