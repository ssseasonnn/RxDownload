package zlc.season.rxdownloadproject.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

/**
 * Target: 界面元素的双向绑定
 */

public class RxDownloadModel extends BaseObservable {
	private String percent = "0.00%";
	private String size = "0.0KB/0.0KB";
	private String status = "";
	private String action = "开始";

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

	@Bindable
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		notifyPropertyChanged(zlc.season.rxdownloadproject.BR.status);
	}

	@Bindable
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
		notifyPropertyChanged(zlc.season.rxdownloadproject.BR.action);
	}
}
