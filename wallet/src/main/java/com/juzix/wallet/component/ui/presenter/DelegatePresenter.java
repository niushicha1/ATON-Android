package com.juzix.wallet.component.ui.presenter;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.juzhen.framework.network.ApiRequestBody;
import com.juzhen.framework.network.ApiResponse;
import com.juzhen.framework.network.ApiSingleObserver;
import com.juzhen.framework.util.NumberParserUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.app.CustomThrowable;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.DelegateContract;
import com.juzix.wallet.component.ui.dialog.InputWalletPasswordDialogFragment;
import com.juzix.wallet.component.ui.dialog.SelectWalletDialogFragment;
import com.juzix.wallet.engine.DelegateManager;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.engine.ServerUtils;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.engine.Web3jManager;
import com.juzix.wallet.entity.AccountBalance;
import com.juzix.wallet.entity.DelegateHandle;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.BigDecimalUtil;
import com.juzix.wallet.utils.RxUtils;

import org.web3j.crypto.Credentials;
import org.web3j.platon.BaseResponse;
import org.web3j.platon.ContractAddress;
import org.web3j.platon.StakingAmountType;
import org.web3j.platon.TransactionCallback;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.PlatonSendTransaction;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.GasProvider;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import retrofit2.Response;

public class DelegatePresenter extends BasePresenter<DelegateContract.View> implements DelegateContract.Presenter {
    private Wallet mWallet;
    private String mNodeAddress;
    private String mNodeName;
    private String mNodeIcon;
    private int tag;//获取是从哪个页面跳转到委托页

    public DelegatePresenter(DelegateContract.View view) {
        super(view);
        mNodeAddress = view.getNodeAddressFromIntent();
        mNodeName = view.getNodeNameFromIntent();
        mNodeIcon = view.getNodeIconFromIntent();
        tag = view.getJumpTagFromIntent();
    }

    @Override
    public void showSelectWalletDialogFragment() {
        SelectWalletDialogFragment.newInstance(mWallet != null ? mWallet.getUuid() : "", true)
                .setOnItemClickListener(new SelectWalletDialogFragment.OnItemClickListener() {
                    @Override
                    public void onItemClick(Wallet walletEntity) {
                        if (isViewAttached()) {
                            mWallet = walletEntity;
                            getView().showSelectedWalletInfo(walletEntity);
                        }
                    }
                })
                .show(currentActivity().getSupportFragmentManager(), "showSelectWalletDialog");

    }

    @Override
    public void showWalletInfo() {
        getView().showNodeInfo(mNodeAddress, mNodeName, mNodeIcon);
        showDefaultWalletInfo();
        getWalletBalance();

    }

    @Override
    public String checkDelegateAmount(String delegateAmount) {
        double amount = NumberParserUtils.parseDouble(delegateAmount);
        //检查委托的数量
        String errMsg = null;
        if (TextUtils.isEmpty(delegateAmount)) {
            errMsg = string(R.string.transfer_amount_cannot_be_empty);
        } else if (amount < 10) {
            //按钮不可点击,并且下方提示
            getView().showTips(true);
            updateDelegateButtonState();
        } else {
            getView().showTips(false);
            updateDelegateButtonState();
        }
        return delegateAmount;

    }

    @Override
    public void updateDelegateButtonState() {
        if (isViewAttached()) {
            String withdrawAmount = getView().getDelegateAmount(); //获取输入的委托数量
            boolean isAmountValid = !TextUtils.isEmpty(withdrawAmount) && NumberParserUtils.parseDouble(withdrawAmount) > 10;
            getView().setDelegateButtonState(isAmountValid);
        }

    }

    private void showDefaultWalletInfo() {
        mWallet = WalletManager.getInstance().getFirstValidIndividualWalletBalance();
        if (isViewAttached() && mWallet != null) {
            getView().showSelectedWalletInfo(mWallet);
        }
    }

    /**
     * 获取所有钱包的余额(可用余额+锁仓余额)
     */
    private void getWalletBalance() {
        List<String> walletAddressList = WalletManager.getInstance().getAddressList();
        ServerUtils.getCommonApi().getAccountBalance(NodeManager.getInstance().getChainId(), ApiRequestBody.newBuilder()
                .put("addrs", walletAddressList.toArray(new String[walletAddressList.size()]))
                .build())
                .compose(RxUtils.bindToLifecycle(getView()))
                .compose(RxUtils.getSingleSchedulerTransformer())
                .subscribe(new ApiSingleObserver<List<AccountBalance>>() {
                    @Override
                    public void onApiSuccess(List<AccountBalance> accountBalances) {
                        if (isViewAttached()) {
                            if (null != accountBalances && accountBalances.size() > 0) {
                                getView().getWalletBalanceList(accountBalances);
                            }

                        }
                    }

                    @Override
                    public void onApiFailure(ApiResponse response) {

                    }
                });

    }

    @Override
    public void checkIsCanDelegate() {
        ServerUtils.getCommonApi().getIsDelegateInfo(NodeManager.getInstance().getChainId(), ApiRequestBody.newBuilder()
                .put("addr", "") // mWallet.getPrefixAddress() todo  暂时写个空
                .put("nodeId", mNodeAddress)
                .build())
                .compose(RxUtils.bindToLifecycle(getView()))
                .compose(RxUtils.getSingleSchedulerTransformer())
                .subscribe(new ApiSingleObserver<DelegateHandle>() {
                    @Override
                    public void onApiSuccess(DelegateHandle delegateHandle) {
                        if (isViewAttached()) {
                            if (null != delegateHandle) {
                                getView().showIsCanDelegate(delegateHandle);
                            }

                        }

                    }

                    @Override
                    public void onApiFailure(ApiResponse response) {

                    }
                });
    }

    //获取手续费
    @SuppressLint("CheckResult")
    @Override
    public void getGasPrice(String chooseType) {
        String inputAmount = getView().getDelegateAmount();//输入的数量
        DelegateManager.getInstance().getDelegateGasPrice(mNodeAddress, chooseType, inputAmount)
                .compose(RxUtils.bindToLifecycle(getView()))
                .compose(RxUtils.getSingleSchedulerTransformer())
                .subscribe(new Consumer<GasProvider>() {
                    @Override
                    public void accept(GasProvider gasProvider) throws Exception {
                        if (isViewAttached()) {
                            BigDecimal gas = BigDecimalUtil.mul(gasProvider.getGasLimit().toString(), gasProvider.getGasPrice().toString());
                            getView().showGasPrice(String.valueOf(gas.intValue()));
                        }
                    }
                });

    }


    @SuppressLint("CheckResult")
    @Override
    public void submitDelegate(String type) {

        Single.fromCallable(new Callable<Double>() {
            @Override
            public Double call() throws Exception {
                //输入数量+手续费
                String inputNumber = getView().getDelegateAmount();
                String gasNumber = getView().getGas();
                return BigDecimalUtil.add(inputNumber, gasNumber);
            }
        }).zipWith(ServerUtils.getCommonApi().getIsDelegateInfo(NodeManager.getInstance().getChainId(), ApiRequestBody.newBuilder()
                        .put("addr", mWallet.getPrefixAddress())
                        .put("nodeId", mNodeAddress)
                        .build()),
                new BiFunction<Double, Response<ApiResponse<DelegateHandle>>, String>() {
                    @Override
                    public String apply(Double amount, Response<ApiResponse<DelegateHandle>> apiResponseResponse) throws Exception {
                        if (apiResponseResponse == null || !apiResponseResponse.isSuccessful()) {
                            return "";
                        } else {
                            DelegateHandle data = apiResponseResponse.body().getData();
                            String isDelegate = "0"; //0 表示false,1 表示true
                            if (!data.isCanDelegation()) {
                                isDelegate = "0";
                            } else {
                                isDelegate = "1";
                            }

                            String message = data.getMessage();

                            return amount + "&" + isDelegate + "&" + message;
                        }

                    }
                })
                .compose(bindToLifecycle())
                .compose(RxUtils.getSingleSchedulerTransformer())
//                .compose(LoadingTransformer.bindToSingleLifecycle(currentActivity()))
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String result) throws Exception {
                        if (isViewAttached()) {
                            double amount = NumberParserUtils.parseDouble(result.split("&", 3)[0]);
                            String delegateState = result.split("&", 3)[1];
                            String msg = result.split("&", 3)[2];

                            double chooseAmount = NumberParserUtils.parseDouble(getView().getChooseBalance()); //选择的余额
                            if (chooseAmount < amount) {
                                //余额不足
                                showLongToast(R.string.insufficient_balance_unable_to_delegate);
                                return;
                            }

                            //不能委托
                            if (TextUtils.equals(delegateState, "0")) {
                                //表示不能委托
                                if (TextUtils.equals(msg, "1")) { //不能委托原因：解除委托金额大于0
                                    showLongToast(R.string.delegate_no_click);
                                    return;
                                } else { //节点已退出或退出中
                                    showLongToast(R.string.the_Validator_has_exited_and_cannot_be_delegated);
                                    return;
                                }
                            }

                            String inputAmount = getView().getDelegateAmount();

                            InputWalletPasswordDialogFragment
                                    .newInstance(mWallet)
                                    .setOnWalletPasswordCorrectListener(new InputWalletPasswordDialogFragment.OnWalletPasswordCorrectListener() {
                                        @Override
                                        public void onWalletPasswordCorrect(Credentials credentials) {
                                            delegate(credentials, inputAmount, mNodeAddress, type);
                                        }
                                    })
                                    .show(currentActivity().getSupportFragmentManager(), "inputWalletPasssword");

                        }


                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (isViewAttached()) {
                            if (throwable instanceof CustomThrowable) {
                                CustomThrowable customThrowable = (CustomThrowable) throwable;
                                showLongToast(customThrowable.getDetailMsgRes());
                            } else {
                                showLongToast(R.string.delegate_failed);
                            }
                        }
                    }
                });

    }


    @SuppressLint("CheckResult")
    private void delegate(Credentials credentials, String inputAmount, String address, String type) {
        Web3j web3j = Web3jManager.getInstance().getWeb3j();
        String chainId = NodeManager.getInstance().getChainId();
        org.web3j.platon.contracts.DelegateContract delegateContract = org.web3j.platon.contracts.DelegateContract.load(web3j, credentials, new DefaultGasProvider(), chainId);
        StakingAmountType stakingAmountType = TextUtils.equals(type, "balance") ? StakingAmountType.FREE_AMOUNT_TYPE : StakingAmountType.RESTRICTING_AMOUNT_TYPE;
        delegateContract.asyncDelegate(address, stakingAmountType, new BigInteger(inputAmount), new TransactionCallback<BaseResponse>() {
            @Override
            public void onTransactionStart() {

            }

            @Override
            public void onTransaction(PlatonSendTransaction sendTransaction) {

            }

            @Override
            public void onTransactionSucceed(BaseResponse response) {
                if (response != null && response.isStatusOk()) {
                    //交易成功，关闭当前页面，并返回到交易详情
                    if (isViewAttached()) {
                        //todo 交易手续费暂时还没拿
                        getView().transactionSuccessInfo(mWallet.getPrefixAddress(), ContractAddress.DELEGATE_CONTRACT_ADDRESS, 0, "1004", inputAmount, "", mNodeName, mNodeAddress
                                , 2);
                    }

                }


            }

            @Override
            public void onTransactionFailed(BaseResponse baseResponse) {

            }
        });


//        DelegateManager.getInstance().delegate(credentials, inputAmount, address, type)
//                .compose(RxUtils.getSingleSchedulerTransformer())
//                .subscribe(new Consumer<BaseResponse>() {
//                    @Override
//                    public void accept(BaseResponse baseResponse) throws Exception {
//                        if (isViewAttached()) {
//                            if (baseResponse != null && baseResponse.isStatusOk()) {
//                                //委托成功
//                                showLongToast(R.string.delegate_success);
//                                //发送一个eventbus
//                                if (tag == 0) {
//                                    EventPublisher.getInstance().sendUpdateDelegateEvent();
//                                } else {
//                                    EventPublisher.getInstance().sendUpdateValidatorsDetailEvent();
//                                }
//                                currentActivity().finish();
//                            }
//
//                        }
//
//
//                    }
//                }, new Consumer<Throwable>() {
//                    @Override
//                    public void accept(Throwable throwable) throws Exception {
//                        if (isViewAttached()) {
//                            showLongToast(R.string.delegate_failed);
//                        }
//                    }
//                });


    }


}
