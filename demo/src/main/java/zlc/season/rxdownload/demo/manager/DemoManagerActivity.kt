package zlc.season.rxdownload.demo.manager

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_demo_list.*
import kotlinx.android.synthetic.main.demo_download_list_item.*
import zlc.season.rxdownload.demo.R
import zlc.season.rxdownload.demo.utils.click
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
                res(R.layout.demo_download_list_item)

                onBind {
                    tv_name.text = data.task.taskName

                    data.renderStatus(data.status,
                            progress_bar,
                            tv_status,
                            tv_percent,
                            btn_start,
                            btn_pause,
                            btn_cancel,
                            btn_more,
                            containerView.context)

                    btn_start.click {
                        data.start()
                    }

                    btn_pause.click {
                        data.stop()
                    }

                    btn_cancel.click {
                        data.cancel()
                        dataSource.removeItem(data)
                    }
                }

                onAttach {
                    data.subscribe(
                            progress_bar,
                            tv_status,
                            tv_percent,
                            btn_start,
                            btn_pause,
                            btn_cancel,
                            btn_more,
                            containerView.context
                    )
                }

                onDetach {
                    data.dispose()
                }
            }
        }
    }
}
