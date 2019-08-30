package zlc.season.rxdownload.demo.list

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import zlc.season.rxdownload.demo.utils.mock_json
import zlc.season.yasha.YashaDataSource
import zlc.season.yasha.YashaItem

class DemoListDataSource : YashaDataSource() {
    override fun loadInitial(loadCallback: LoadCallback<YashaItem>) {
        val type = object : TypeToken<List<DemoListItem>>() {}.type
        val mockData = Gson().fromJson<List<DemoListItem>>(mock_json, type)
        loadCallback.setResult(mockData)
    }
}