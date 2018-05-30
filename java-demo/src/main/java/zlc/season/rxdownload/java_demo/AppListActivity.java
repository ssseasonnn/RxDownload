package zlc.season.rxdownload.java_demo;


import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import zlc.season.rxdownload.java_demo.databinding.ActivityAppListBinding;
import zlc.season.rxdownload.java_demo.databinding.ViewHolderAppItemBinding;
import zlc.season.rxdownload3.RxDownload;
import zlc.season.rxdownload3.core.Deleted;
import zlc.season.rxdownload3.core.Downloading;
import zlc.season.rxdownload3.core.Failed;
import zlc.season.rxdownload3.core.Normal;
import zlc.season.rxdownload3.core.Status;
import zlc.season.rxdownload3.core.Succeed;
import zlc.season.rxdownload3.core.Suspend;
import zlc.season.rxdownload3.core.Waiting;
import zlc.season.rxdownload3.extension.ApkInstallExtension;
import zlc.season.rxdownload3.extension.ApkOpenExtension;
import zlc.season.rxdownload3.helper.UtilsKt;

public class AppListActivity extends AppCompatActivity {
    private ActivityAppListBinding mainBinding;
    private Adapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_app_list);
        mainBinding.toolbar.inflateMenu(R.menu.menu_for_app_list);
        mainBinding.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(AppListActivity.this, DownloadListActivity.class));
                return true;
            }
        });

        adapter = new Adapter();
        mainBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.recyclerView.setAdapter(adapter);

        addData();
    }


    private void addData() {
        List<CustomMission> data = new ArrayList<>();
        String[] images = getResources().getStringArray(R.array.image);
        String[] urls = getResources().getStringArray(R.array.url);
        String[] introduces = getResources().getStringArray(R.array.introduce);
        for (int i = 0; i < images.length; i++) {
            data.add(new CustomMission(urls[i], introduces[i], images[i]));
        }
        adapter.addData(data);

        createAllMissionOnStart(data);
    }

    private void createAllMissionOnStart(List<CustomMission> data) {
        RxDownload.INSTANCE.createAll(data, false).subscribe();
    }

    static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private List<CustomMission> data = new ArrayList<>();

        public void addData(List<CustomMission> data) {
            this.data.clear();
            this.data.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ViewHolderAppItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.view_holder_app_item, parent, false);
            return new ViewHolder(binding.getRoot());
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.setData(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.onAttach();
        }

        @Override
        public void onViewDetachedFromWindow(ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.onDetach();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private CustomMission customMission;
        private Disposable disposable;
        private Status currentStatus;

        private ViewHolderAppItemBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
            binding.action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchClick();
                }
            });
        }

        private void dispatchClick() {
            if (currentStatus instanceof Normal) {
                start();
            } else if (currentStatus instanceof Suspend) {
                start();
            } else if (currentStatus instanceof Failed) {
                start();
            } else if (currentStatus instanceof Downloading) {
                stop();
            } else if (currentStatus instanceof Succeed) {
                install();
            } else if (currentStatus instanceof ApkInstallExtension.Installed) {
                open();
            } else if (currentStatus instanceof Deleted) {
                start();
            }
        }


        public void setData(CustomMission customMission) {
            this.customMission = customMission;
            binding.introduce.setText(customMission.getIntroduce());
            Picasso.with(itemView.getContext()).load(customMission.getImg()).into(binding.icon);
        }

        public void onAttach() {
            disposable = RxDownload.INSTANCE.create(customMission, false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Status>() {
                        @Override
                        public void accept(Status status) throws Exception {
                            currentStatus = status;
                            setActionText(status);
                        }
                    });
        }

        private void setActionText(Status status) {
            String text = "";
            if (status instanceof Normal) {
                text = "开始";
            } else if (status instanceof Suspend) {
                text = "已暂停";
            } else if (status instanceof Waiting) {
                text = "等待中";
            } else if (status instanceof Downloading) {
                text = "暂停";
            } else if (status instanceof Failed) {
                text = "失败";
            } else if (status instanceof Succeed) {
                text = "安装";
            } else if (status instanceof ApkInstallExtension.Installing) {
                text = "安装中";
            } else if (status instanceof ApkInstallExtension.Installed) {
                text = "打开";
            } else if (status instanceof Deleted) {
                text = "开始";
            }
            binding.action.setText(text);
        }

        public void onDetach() {
            UtilsKt.dispose(disposable);
        }

        private void start() {
            RxDownload.INSTANCE.start(customMission).subscribe();
        }

        private void stop() {
            RxDownload.INSTANCE.stop(customMission).subscribe();
        }

        private void install() {
            RxDownload.INSTANCE.extension(customMission, ApkInstallExtension.class).subscribe();
        }

        private void open() {
            RxDownload.INSTANCE.extension(customMission, ApkOpenExtension.class).subscribe();
        }
    }
}
