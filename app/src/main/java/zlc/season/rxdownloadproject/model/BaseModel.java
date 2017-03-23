package zlc.season.rxdownloadproject.model;

import android.databinding.Bindable;

public class BaseModel extends RxDownloadModel {
	private String url = "";
	private String state = "";
	private boolean control = true;

	public BaseModel() {
	}

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
}
