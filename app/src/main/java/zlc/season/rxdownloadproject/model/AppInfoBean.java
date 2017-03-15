package zlc.season.rxdownloadproject.model;

import io.reactivex.disposables.Disposable;
import zlc.season.practicalrecyclerview.ItemType;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 09:43
 * FIXME
 */
public class AppInfoBean implements ItemType {
	public String name;
	public String img;
	public String info;
	public String downloadUrl;
	public String saveName;
	public Disposable disposable;

	public AppInfoBean(String name, String img, String info, String downloadUrl) {
		this.name = name;
		this.img = img;
		this.info = info;
		this.downloadUrl = downloadUrl;
		this.saveName = getSaveNameByUrl(downloadUrl);
	}

	@Override
	public int itemType() {
		return 0;
	}


	/**
	 * 截取Url最后一段作为文件保存名称
	 *
	 * @param url url
	 * @return saveName
	 */
	private String getSaveNameByUrl(String url) {
		return url.substring(url.lastIndexOf('/') + 1);
	}
}
