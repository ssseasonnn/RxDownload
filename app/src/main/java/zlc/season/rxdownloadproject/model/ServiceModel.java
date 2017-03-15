package zlc.season.rxdownloadproject.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

/**
 * Created by xieqi on 2017/3/15.
 */

public class ServiceModel extends BaseObservable {
	private String percent = "0.00%";
	private String size = "0.0KB/0.0KB";

	@Bindable
	public String getPercent() {
		return percent;
	}

	public void setPercent(String percent) {
		this.percent = percent;
		notifyPropertyChanged(zlc.season.rxdownloadproject.BR.percent);
	}

	@Bindable
	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
		notifyPropertyChanged(zlc.season.rxdownloadproject.BR.size);
	}
}
