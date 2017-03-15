package zlc.season.rxdownloadproject.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import zlc.season.rxdownloadproject.BR;

/**
 *
 */
public class BaseModel extends BaseObservable {
	private String url = "";
	private String state = "";
	private String action = "开始";
	private boolean control = true;
	@Bindable
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
//		notifyPropertyChanged(BR.url);
	}

	@Bindable
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
//		notifyPropertyChanged(BR.state);
	}

	@Bindable
	public boolean isControl() {
		return control;
	}

	public void setControl(boolean control) {
		this.control = control;
//		notifyPropertyChanged(BR.control);
	}

	@Bindable
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
		notifyPropertyChanged(BR.action);
	}
}
