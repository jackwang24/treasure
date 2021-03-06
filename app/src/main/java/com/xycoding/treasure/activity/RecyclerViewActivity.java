package com.xycoding.treasure.activity;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xycoding.treasure.R;
import com.xycoding.treasure.adapter.RecyclerViewAdapter;
import com.xycoding.treasure.databinding.ActivityRecyclerViewBinding;
import com.xycoding.treasure.databinding.LayoutRecyclerViewItemBinding;
import com.xycoding.treasure.rx.RxViewWrapper;
import com.xycoding.treasure.view.LoadMoreView;
import com.xycoding.treasure.view.recyclerview.ExpandableRecyclerViewAdapter;
import com.xycoding.treasure.view.recyclerview.GridBottomDividerItemDecoration;
import com.xycoding.treasure.view.recyclerview.ItemDragCallBack;
import com.xycoding.treasure.view.recyclerview.LoadMoreRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;

/**
 * Created by xuyang on 2016/10/31.
 */
public class RecyclerViewActivity extends BaseBindingActivity {

    private ActivityRecyclerViewBinding mBinding;
    private List<String> mData = new ArrayList<>();
    private LoadMoreView mLoadMoreView;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_recycler_view;
    }

    @Override
    protected void initControls(Bundle savedInstanceState) {
        mBinding = (ActivityRecyclerViewBinding) binding;
        initLinearRecyclerView();
        initGridRecyclerView();
        initGroupRecyclerView();
        recyclerViewVisible(0);
    }

    @Override
    protected void setListeners() {
        mLoadMoreView.setReloadClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadMoreItems();
            }
        });
        mBinding.recyclerView.setLoadMoreListener(new LoadMoreRecyclerView.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                loadMoreItems();
            }
        });
        subscriptions.add(RxViewWrapper.clicks(mBinding.btnBorder).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                recyclerViewVisible(0);
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.btnMore).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                recyclerViewVisible(1);
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.btnGroup).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                recyclerViewVisible(2);
            }
        }));
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        addDummyItems();
    }

    private void initLinearRecyclerView() {
        final RecyclerViewAdapter adapter = new RecyclerViewAdapter(mData);
        mBinding.recyclerView.setAdapter(adapter);
        mLoadMoreView = new LoadMoreView(this);
        mBinding.recyclerView.setFooterView(mLoadMoreView);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //drag sort
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemDragCallBack(adapter));
        adapter.setDragListener(new RecyclerViewAdapter.OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
        adapter.setRemoveListener(new RecyclerViewAdapter.OnItemRemoveListener() {
            @Override
            public void onItemRemove(final int position, final String string) {
                Snackbar.make(mBinding.recyclerView, "删除 " + string, Snackbar.LENGTH_LONG)
                        .setAction("撤销", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mData.add(position, string);
                                adapter.notifyItemInserted(position);
                            }
                        }).show();
            }
        });
        itemTouchHelper.attachToRecyclerView(mBinding.recyclerView);
    }

    private void initGridRecyclerView() {
        final RecyclerViewAdapter adapter = new RecyclerViewAdapter(loadMoreDummyItems(5));
        mBinding.recyclerViewGrid.setAdapter(adapter);
        int columns = 3;
        mBinding.recyclerViewGrid.setLayoutManager(new GridLayoutManager(this, columns));
        mBinding.recyclerViewGrid.addItemDecoration(
                new GridBottomDividerItemDecoration(
                        ContextCompat.getDrawable(this, R.drawable.shape_grid_divider),
                        columns));
    }

    private void initGroupRecyclerView() {
        List<ExpandableRecyclerViewAdapter.ExpandableGroup<String, String>> groups = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            List<String> children = new ArrayList<>();
            for (int j = 1; j <= i; j++) {
                children.add("Child : " + i + "_" + j);
            }
            String head = "Group : " + i;
            ExpandableRecyclerViewAdapter.ExpandableGroup<String, String> group =
                    new ExpandableRecyclerViewAdapter.ExpandableGroup<>(head, children);
            groups.add(group);
        }
        final ExpandableRecyclerViewAdapter<String, String, GroupHolder> adapter =
                new ExpandableRecyclerViewAdapter<String, String, GroupHolder>(groups) {
                    @Override
                    public GroupHolder onCreateHeadViewHolder(ViewGroup parent) {
                        return new GroupHolder(
                                LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recycler_view_item, parent, false));
                    }

                    @Override
                    public GroupHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
                        return new GroupHolder(
                                LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recycler_view_item, parent, false));
                    }

                    @Override
                    public void onBindHeadViewHolder(GroupHolder holder, String s, boolean expanded) {
                        holder.binding.layoutContainer.setBackgroundColor(Color.GRAY);
                        holder.binding.tvLabel.setText(s);
                        holder.binding.ivSort.setImageResource(
                                expanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
                    }

                    @Override
                    public void onBindChildViewHolder(GroupHolder holder, String s) {
                        holder.binding.tvLabel.setText(s);
                        holder.binding.ivSort.setVisibility(View.INVISIBLE);
                    }
                };
        mBinding.recyclerViewGroup.setAdapter(adapter);
        mBinding.recyclerViewGroup.setLayoutManager(new LinearLayoutManager(this));
    }

    class GroupHolder extends RecyclerView.ViewHolder {

        private LayoutRecyclerViewItemBinding binding;

        public GroupHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }

    private void recyclerViewVisible(int type) {
        mBinding.recyclerView.setVisibility(View.INVISIBLE);
        mBinding.recyclerViewGrid.setVisibility(View.INVISIBLE);
        mBinding.recyclerViewGroup.setVisibility(View.INVISIBLE);
        switch (type) {
            case 1:
                mBinding.recyclerView.setVisibility(View.VISIBLE);
                break;
            case 2:
                mBinding.recyclerViewGroup.setVisibility(View.VISIBLE);
                break;
            default:
                mBinding.recyclerViewGrid.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void addDummyItems() {
        for (int i = 1; i <= 20; i++) {
            mData.add("Item " + i);
        }
        mBinding.recyclerView.getAdapter().notifyDataSetChanged();
    }

    private List<String> loadMoreDummyItems(int num) {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= num; i++) {
            items.add("Item " + (mData.size() + i));
        }
        return items;
    }

    private void loadMoreItems() {
        subscriptions.add(Observable
                .defer(new Func0<Observable<List<String>>>() {
                    @Override
                    public Observable<List<String>> call() {
                        return Observable.just(loadMoreDummyItems(20));
                    }
                })
                .delay(3000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<String>>() {

                    @Override
                    public void onStart() {
                        mLoadMoreView.showLoadingView();
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        mLoadMoreView.showReLoadView();
                    }

                    @Override
                    public void onNext(List<String> strings) {
                        if (strings == null || strings.size() == 0) {
                            //no more result
                            mLoadMoreView.showNoMoreView();
                        } else {
                            int startPos = mData.size();
                            mData.addAll(strings);
                            mBinding.recyclerView.getAdapter().notifyItemRangeInserted(startPos, strings.size());
                        }
                    }
                }));
    }
}
