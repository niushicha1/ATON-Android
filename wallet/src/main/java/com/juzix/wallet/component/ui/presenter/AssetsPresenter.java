package com.juzix.wallet.component.ui.presenter;

import android.text.TextUtils;

import com.juzhen.framework.network.ApiErrorCode;
import com.juzhen.framework.network.ApiRequestBody;
import com.juzhen.framework.network.ApiResponse;
import com.juzhen.framework.network.ApiSingleObserver;
import com.juzix.wallet.app.CustomObserver;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.AssetsContract;
import com.juzix.wallet.component.ui.dialog.InputWalletPasswordDialogFragment;
import com.juzix.wallet.component.ui.view.BackupMnemonicPhraseActivity;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.engine.ServerUtils;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.engine.Web3jManager;
import com.juzix.wallet.entity.AccountBalance;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.RxUtils;
import com.trello.rxlifecycle2.android.FragmentEvent;

import org.reactivestreams.Publisher;
import org.web3j.crypto.Credentials;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import retrofit2.Response;

public class AssetsPresenter extends BasePresenter<AssetsContract.View> implements AssetsContract.Presenter {

    private static final String TAG = AssetsPresenter.class.getSimpleName();

    private List<Wallet> mWalletList;

    public AssetsPresenter(AssetsContract.View view) {
        super(view);
        mWalletList = WalletManager.getInstance().getWalletList();
    }

    @Override
    public void fetchWalletList() {
        if (isViewAttached()) {
            show();
        }
    }

    @Override
    public List<Wallet> getRecycleViewDataSource() {
        return mWalletList;
    }

    @Override
    public void clickRecycleViewItem(Wallet walletEntity) {
        WalletManager.getInstance().setSelectedWallet(walletEntity);
        getView().showWalletList(walletEntity);
        getView().showWalletInfo(walletEntity);
        getView().setArgument(walletEntity);
    }

    @Override
    public void backupWallet() {
        Wallet walletEntity = WalletManager.getInstance().getSelectedWallet();
        InputWalletPasswordDialogFragment.newInstance(walletEntity).setOnWalletCorrectListener(new InputWalletPasswordDialogFragment.OnWalletCorrectListener() {
            @Override
            public void onCorrect(Credentials credentials, String password) {
                BackupMnemonicPhraseActivity.actionStart(getContext(), password, walletEntity, 1);
            }
        }).show(currentActivity().getSupportFragmentManager(), "inputPassword");
    }

    @Override
    public void fetchWalletsBalance() {

//
//        ServerUtils
//                .getCommonApi()
//                .getAccountBalance(NodeManager.getInstance().getChainId(), ApiRequestBody.newBuilder()
//                        .put("addrs", WalletManager.getInstance().getAddressList())
//                        .build())
//                .toFlowable()
//                .flatMap(new Function<Response<ApiResponse<List<AccountBalance>>>, Publisher<AccountBalance>>() {
//                    @Override
//                    public Publisher<AccountBalance> apply(Response<ApiResponse<List<AccountBalance>>> apiResponseResponse) throws Exception {
//                        if (apiResponseResponse != null && apiResponseResponse.isSuccessful() && apiResponseResponse.body().getResult() == ApiErrorCode.SUCCESS) {
//                            return Flowable.fromIterable(apiResponseResponse.body().getData());
//                        }
//                        return Flowable.error(new Throwable());
//                    }
//                })
//                .map(new Function<AccountBalance, AccountBalance>() {
//
//                    @Override
//                    public AccountBalance apply(AccountBalance accountBalance) throws Exception {
//                        double balance = WalletManager.getInstance().getWalletAmountByAddress(accountBalance.getAddr());
//                        return accountBalance.get;
//                    }
//                })
//                .doOnNext(new Consumer<AccountBalance>() {
//                    @Override
//                    public void accept(AccountBalance accountBalance) throws Exception {
//
//                    }
//                });


        Flowable
                .fromIterable(mWalletList)
                .map(new Function<Wallet, Double>() {
                    @Override
                    public Double apply(Wallet walletEntity) throws Exception {
                        double balance = Web3jManager.getInstance().getBalance(walletEntity.getPrefixAddress());
                        walletEntity.setBalance(balance == 0 ? walletEntity.getBalance() : balance);
                        return balance == 0 ? walletEntity.getBalance() : balance;
                    }
                })
                .reduce(new BiFunction<Double, Double, Double>() {
                    @Override
                    public Double apply(Double aDouble, Double aDouble2) throws Exception {
                        return aDouble + aDouble2;
                    }
                })
                .toObservable()
                .compose(bindUntilEvent(FragmentEvent.STOP))
                .compose(RxUtils.getSchedulerTransformer())
                .subscribe(new CustomObserver<Double>() {
                    @Override
                    public void accept(Double balance) {
                        if (isViewAttached()) {
                            getView().showTotalBalance(balance);
                            Wallet walletEntity = WalletManager.getInstance().getSelectedWallet();
                            if (walletEntity != null) {
                                getView().showBalance(walletEntity.getBalance());
                            }
                            getView().finishRefresh();
                        }
                    }

                    @Override
                    public void accept(Throwable throwable) {
                        super.accept(throwable);
                        if (isViewAttached()) {
                            getView().finishRefresh();
                        }
                    }
                });
    }


    private boolean isSelected(Wallet selectedWallet) {
        if (selectedWallet == null) {
            return false;
        }
        for (int i = 0; i < mWalletList.size(); i++) {
            if (mWalletList.get(i) == selectedWallet) {
                return true;
            }
        }
        return false;
    }

    private Wallet getSelectedWallet() {
        for (int i = 0; i < mWalletList.size(); i++) {
            return mWalletList.get(i);
        }
        return null;
    }

    private void show() {
        if (mWalletList.isEmpty()) {
            getView().showTotalBalance(0);
            getView().showContent(true);
            return;
        }
        Collections.sort(mWalletList);
        Wallet walletEntity = WalletManager.getInstance().getSelectedWallet();
        if (!isSelected(walletEntity)) {
            //挑选一个当前选中的钱包
            walletEntity = getSelectedWallet();
            WalletManager.getInstance().setSelectedWallet(walletEntity);
            getView().setArgument(walletEntity);
        }
        getView().showWalletList(walletEntity);
        getView().showWalletInfo(walletEntity);
        getView().showContent(false);
    }

    private Single<Response<ApiResponse<List<AccountBalance>>>> getAccountBalanceList() {

        return ServerUtils.getCommonApi().getAccountBalance(NodeManager.getInstance().getChainId(), ApiRequestBody.newBuilder()
                .put("addrs", WalletManager.getInstance().getAddressList())
                .build());
    }
}
