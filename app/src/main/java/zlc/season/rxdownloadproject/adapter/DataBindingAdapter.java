package zlc.season.rxdownloadproject.adapter;

import android.databinding.BindingAdapter;
import android.widget.ImageView;

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
}
