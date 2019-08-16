package com.juzix.wallet.component.ui.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.juzix.wallet.App;
import com.juzix.wallet.R;
import com.juzix.wallet.app.CustomObserver;
import com.juzix.wallet.component.adapter.MyDelegateAdapter;
import com.juzix.wallet.component.ui.base.MVPBaseFragment;
import com.juzix.wallet.component.ui.contract.MyDelegateContract;
import com.juzix.wallet.component.ui.presenter.MyDelegatePresenter;
import com.juzix.wallet.component.widget.CustomRefreshHeader;
import com.juzix.wallet.component.widget.headerandfooter.HeaderAndFooterAdapter;
import com.juzix.wallet.component.widget.headerandfooter.HeaderAndFooterRecyclerView;
import com.juzix.wallet.config.AppSettings;
import com.juzix.wallet.entity.DelegateInfo;
import com.juzix.wallet.entity.MyDelegate;
import com.juzix.wallet.event.Event;
import com.juzix.wallet.event.EventPublisher;
import com.juzix.wallet.utils.GlideUtils;
import com.juzix.wallet.utils.RxUtils;
import com.juzix.wallet.utils.StringUtil;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * 我的委托页面
 */

public class MyDelegateFragment extends MVPBaseFragment<MyDelegatePresenter> implements MyDelegateContract.View {
    private Unbinder unbinder;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.iv_total_delegate)
    ImageView iconDelegate;
    @BindView(R.id.tv_total_delegate)
    TextView tv_total_delegate;
    @BindView(R.id.tv_delegate_record)
    TextView tv_delegate_record;
    @BindView(R.id.ll_no_data)
    LinearLayout ll_no_data;
    @BindView(R.id.list_delegate)
    HeaderAndFooterRecyclerView list_delegate;
    @BindView(R.id.ll_problem)
    LinearLayout ll_problem;
    @BindView(R.id.ll_tutorial)
    LinearLayout ll_tutorial;
    @BindView(R.id.ll_guide)
    LinearLayout ll_guide;

    private MyDelegateAdapter mMyDelegateAdapter;
    private LinearLayoutManager layoutManager;
    List<DelegateInfo> datalist;

    @Override
    protected MyDelegatePresenter createPresenter() {
        return new MyDelegatePresenter(this);
    }

    @Override
    protected void onFragmentPageStart() {
        mPresenter.loadMyDelegateData();
    }

    @Override
    protected View onCreateFragmentPage(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_delegate, container, false);
        unbinder = ButterKnife.bind(this, view);
        EventPublisher.getInstance().register(this);
        initViews();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initClickListener();
    }

    private void initClickListener() {
        RxView.clicks(tv_delegate_record)
                .compose(RxUtils.bindToLifecycle(this))
                .compose(RxUtils.getClickTransformer())
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object o) {
                        DelegateRecordActivity.actionStart(getContext());
                    }
                });
        RxView.clicks(ll_problem).compose(RxUtils.bindToLifecycle(this))
                .compose(RxUtils.getClickTransformer())
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object o) {
                        //todo 暂时写的一个假的链接
//                        CommonHybridActivity.actionStart(getContext(), "https://www.baidu.com");
                        DelegateActivity.actionStart(getContext(),"","","",0);
                    }
                });

        RxView.clicks(ll_tutorial).compose(RxUtils.bindToLifecycle(this))
                .compose(RxUtils.getClickTransformer())
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object o) {
                        //todo 暂时写的一个假链接
                        CommonHybridActivity.actionStart(getContext(), "https://www.baidu.com");
                    }
                });


    }


    private void initViews() {
        initRefreshView();
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mMyDelegateAdapter = new MyDelegateAdapter(datalist);
        list_delegate.setLayoutManager(layoutManager);
        View footerView = LayoutInflater.from(getContext()).inflate(R.layout.include_my_delegate_footer, list_delegate, false);
//        list_delegate.addFooterView(footerView);
        list_delegate.setAdapter(mMyDelegateAdapter);

        list_delegate.setEnabled(true);
        mMyDelegateAdapter.setOnItemClickListener(new MyDelegateAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String walletAddress, String walletName, String walletIcon) {
                //跳转到委托详情页
                DelegateDetailActivity.actionStart(getContext(), walletAddress, walletName, walletIcon);
            }
        });

        Log.d("MyDelegateFragment", "======获取recyclerview的滚动状态===================" + list_delegate.getScrollState());

        if (list_delegate.getScrollY() == 0) {
            ll_guide.setVisibility(View.VISIBLE);
            list_delegate.removeFooterView(footerView);
        }

        list_delegate.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.d("MyDelegateFragment", "======获取recyclerview的滚动状态=======onScrollStateChanged============" + newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                Log.d("MyDelegateFragment", "======获取recyclerview的滚动状态=======onScrollxxxxxxxxxxxxxxx============" + list_delegate.getScrollState());

//                Log.d("MyDelegateFragment", "滚动的距离" + "==========================" + dy + "=====================" + dx);
                list_delegate.removeFooterView(footerView);
                if (dy > 0 || dy < 0) { //表示有滚动的状态此时
                    list_delegate.addFooterView(footerView);
                    ll_guide.setVisibility(View.GONE);

                } else {
                    list_delegate.removeFooterView(footerView);
                    ll_guide.setVisibility(View.VISIBLE);
                }

            }
        });


    }

    private void initRefreshView() {
        refreshLayout.setRefreshHeader(new CustomRefreshHeader(getContext()));
        refreshLayout.setEnableLoadMore(false);//禁止上拉加载更多
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                mPresenter.loadMyDelegateData();
            }
        });
    }


    @Override
    public void showMyDelegateData(List<DelegateInfo> list) {
        if (list != null && list.size() > 0) {
            mMyDelegateAdapter.notifyDataChanged(list);
            ll_no_data.setVisibility(View.GONE);


        } else {
            ll_no_data.setVisibility(View.VISIBLE);
        }

        //todo 有数据的时候可以在这里处理看看 footer

        refreshLayout.finishRefresh();

    }

    @Override
    public void showMyDelegateDataByPosition(int positon, DelegateInfo delegateInfo) {
        //刷新某个item
        mMyDelegateAdapter.notifyItemDataChanged(positon, delegateInfo);
        refreshLayout.finishRefresh();

    }

    @Override
    public void showMyDelegateDataFailed() {
        refreshLayout.finishRefresh();
        mMyDelegateAdapter.notifyDataSetChanged();
    }

    @Override
    public void showTotalDelegate(double total) {
        tv_total_delegate.setText(total > 0 ? StringUtil.formatBalance(total, false) : "0.00");
    }


    //接收event事件然后刷新
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshMyDelegate(Event.UpdateDelegateTabEvent tabEvent) {
        if (AppSettings.getInstance().getMydelegateTab()) {
            refreshLayout.autoRefresh();
        }

    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            AppSettings.getInstance().setMydelegateTab(true);
        } else {
            AppSettings.getInstance().setMydelegateTab(false);
        }

    }
}
