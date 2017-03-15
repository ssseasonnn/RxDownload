package zlc.season.rxdownloadproject.adapter;

import android.annotation.TargetApi;
import android.databinding.BindingAdapter;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.squareup.picasso.Picasso;

/**
 * Target: 为databinding补充绑定方法
 */
public class DataBindingAdapter {
	@BindingAdapter("image")
	public static void setImage(ImageView view, String url) {
		Picasso.with(view.getContext())
				.load(url)
				.into(view);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@BindingAdapter("title")
	public static void setTitle(Toolbar toolbar, String resource) {
		toolbar.setTitle(resource);
	}
}
