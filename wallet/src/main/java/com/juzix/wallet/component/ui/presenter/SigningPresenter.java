package com.juzix.wallet.component.ui.presenter;

import com.juzhen.framework.network.NetConnectivity;
import com.juzhen.framework.util.NumberParserUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.app.CustomThrowable;
import com.juzix.wallet.app.LoadingTransformer;
import com.juzix.wallet.app.SchedulersTransformer;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.SigningContract;
import com.juzix.wallet.component.ui.dialog.InputWalletPasswordDialogFragment;
import com.juzix.wallet.component.ui.dialog.SendTransactionDialogFragment;
import com.juzix.wallet.engine.SharedWalletTransactionManager;
import com.juzix.wallet.engine.Web3jManager;
import com.juzix.wallet.entity.IndividualWalletEntity;
import com.juzix.wallet.entity.SharedTransactionEntity;
import com.juzix.wallet.entity.TransactionResult;
import com.juzix.wallet.utils.BigDecimalUtil;
import com.juzix.wallet.utils.ToastUtil;

import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * @author matrixelement
 */
public class SigningPresenter extends BasePresenter<SigningContract.View> implements SigningContract.Presenter {

    private static final double DEFAULT_WEI = 1E18;
    private static final int CONFIRM = 1;
    private static final int REFUSE = 2;
    private SharedTransactionEntity transactionEntity;
    private IndividualWalletEntity individualWalletEntity;

    public SigningPresenter(SigningContract.View view) {
        super(view);
    }

    @Override
    public void init() {
        transactionEntity = getView().getTransactionFromIntent();
        individualWalletEntity = getView().getIndividualWalletFromIntent();
    }

    @Override
    public void fetchTransactionDetail() {
        if (isViewAttached() && transactionEntity != null) {
            List<TransactionResult> resultList = transactionEntity.getTransactionResult();
            List<TransactionResult> confirmList = new ArrayList<>();
            List<TransactionResult> revokeList = new ArrayList<>();
            List<TransactionResult> undeterminedList = new ArrayList<>();
            String walletAddress = transactionEntity.getContractAddress();
            boolean subTransactoined = true;
            int len = resultList.size();
            for (int i = 0; i < len; i++) {
                TransactionResult result = resultList.get(i);
                TransactionResult.Status status = result.getStatus();
                if (walletAddress.contains(result.getAddress()) && status != TransactionResult.Status.OPERATION_UNDETERMINED) {
                    subTransactoined = false;
                }
                switch (status) {
                    case OPERATION_APPROVAL:
                        confirmList.add(result);
                        break;
                    case OPERATION_REVOKE:
                        revokeList.add(result);
                        break;
                    case OPERATION_UNDETERMINED:
                        undeterminedList.add(result);
                        break;
                }
            }
            if (!resultList.isEmpty()) {
                resultList.clear();
            }
            if (!confirmList.isEmpty()) {
                resultList.addAll(confirmList);
            }
            if (!revokeList.isEmpty()) {
                resultList.addAll(revokeList);
            }
            if (!undeterminedList.isEmpty()) {
                resultList.addAll(undeterminedList);
            }

            String statusDesc = string(R.string.transactionConfirmation) + "(" + confirmList.size() + "/" + transactionEntity.getRequiredSignNumber() + ")";
            getView().setTransactionDetailInfo(transactionEntity, statusDesc);
            getView().showTransactionResult(resultList);

            if (individualWalletEntity != null && isWaittingSigned(transactionEntity.getTransactionResult()) && subTransactoined) {
                getView().enableButtons(true);
            } else {
                getView().enableButtons(false);
            }
        }
    }

    private boolean isWaittingSigned(List<TransactionResult> transactionResultList) {

        if (transactionResultList == null || transactionResultList.isEmpty()) {
            return false;
        }

        for (TransactionResult result : transactionResultList) {
            if (individualWalletEntity.getPrefixAddress().equals(result.getPrefixAddress()) && result.getStatus() == TransactionResult.Status.OPERATION_UNDETERMINED) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void confirm() {
        if (isViewAttached()) {
            start(CONFIRM);
        }
    }

    @Override
    public void revoke() {
        if (isViewAttached()) {
            start(REFUSE);
        }
    }

    private void start(int type) {
        if (!NetConnectivity.getConnectivityManager().isConnected()) {
            ToastUtil.showLongToast(currentActivity(), string(R.string.network_error));
            return;
        }

        getGas(type);
    }

    private void showInputWalletPasswordDialogFragment(SharedTransactionEntity sharedTransactionEntity, int type, BigInteger gasPrice, double feeAmount) {
        InputWalletPasswordDialogFragment.newInstance(individualWalletEntity).setOnWalletPasswordCorrectListener(new InputWalletPasswordDialogFragment.OnWalletPasswordCorrectListener() {
            @Override
            public void onWalletPasswordCorrect(Credentials credentials) {
                validPassword(credentials, sharedTransactionEntity, type, gasPrice, feeAmount);

            }
        }).show(currentActivity().getSupportFragmentManager(), "inputPassword");
    }

    private void getGas(int type) {

        Single
                .fromCallable(new Callable<BigInteger>() {
                    @Override
                    public BigInteger call() throws Exception {
                        BigInteger gasPrice = Web3jManager.getInstance().getWeb3j().ethGasPrice().send().getGasPrice();
                        return gasPrice;
                    }
                })
                .compose(new SchedulersTransformer())
                .compose(LoadingTransformer.bindToSingleLifecycle(getView().currentActivity()))
                .compose(bindToLifecycle())
                .subscribe(new Consumer<BigInteger>() {
                    @Override
                    public void accept(BigInteger gasPrice) throws Exception {
                        double gasLimit = type == 1 ? SharedWalletTransactionManager.APPROVE_GAS_LIMIT.doubleValue() : SharedWalletTransactionManager.REVOKE_GAS_LIMIT.doubleValue();
                        final double feeAmount = BigDecimalUtil.div(BigDecimalUtil.mul(gasPrice.doubleValue(), gasLimit), DEFAULT_WEI);
                        SendTransactionDialogFragment
                                .newInstance(string(R.string.execute_contract_confirm), NumberParserUtils.getPrettyBalance(feeAmount), buildTransactionInfo(individualWalletEntity.getName()))
                                .setOnConfirmBtnClickListener(new SendTransactionDialogFragment.OnConfirmBtnClickListener() {
                                    @Override
                                    public void onConfirmBtnClick() {
                                        showInputWalletPasswordDialogFragment(transactionEntity, type, gasPrice, feeAmount);
                                    }
                                })
                                .show(currentActivity().getSupportFragmentManager(), "sendTransaction");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    private Single<Credentials> checkBalance(IndividualWalletEntity individualWalletEntity, Credentials credentials, double feeAmount) {
        return Single.create(new SingleOnSubscribe<Credentials>() {
            @Override
            public void subscribe(SingleEmitter<Credentials> emitter) throws Exception {
                double balance = Web3jManager.getInstance().getBalance(individualWalletEntity.getPrefixAddress());
                if (balance < feeAmount) {
                    emitter.onError(new CustomThrowable(CustomThrowable.CODE_ERROR_NOT_SUFFICIENT_BALANCE));
                } else {
                    emitter.onSuccess(credentials);
                }
            }
        });
    }

    private void validPassword(Credentials credentials, SharedTransactionEntity sharedTransactionEntity, int type, BigInteger gasPrice, double feeAmount) {
                checkBalance(individualWalletEntity, credentials, feeAmount)
                .compose(new SchedulersTransformer())
                        .compose(LoadingTransformer.bindToSingleLifecycle(getView().currentActivity()))
                .compose(bindToLifecycle())
                .subscribe(new Consumer<Credentials>() {
                    @Override
                    public void accept(Credentials credentials) throws Exception {
                        sendTransaction(sharedTransactionEntity, credentials, type, gasPrice);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable instanceof CustomThrowable) {
                            CustomThrowable execption = (CustomThrowable) throwable;
                            if (execption.getErrCode() == CustomThrowable.CODE_ERROR_NOT_SUFFICIENT_BALANCE) {
                                if (isViewAttached()) {
                                    showLongToast(execption.getDetailMsgRes());
                                }
                            }
                        }
                    }
                });


    }

    private void sendTransaction(SharedTransactionEntity sharedTransactionEntity, Credentials credentials, int type, BigInteger gasPrice) {

        SharedWalletTransactionManager.getInstance()
                .sendTransaction(sharedTransactionEntity, credentials, transactionEntity.getContractAddress(), transactionEntity.getTransactionId(), gasPrice, type)
                .compose(new SchedulersTransformer())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        if (isViewAttached()) {
                            getView().updateSigningStatus(individualWalletEntity.getPrefixAddress(), TransactionResult.Status.OPERATION_SIGNING);
                        }
                    }
                })
                .subscribe(new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        if (isViewAttached()) {
                            getView().updateSigningStatus(individualWalletEntity.getPrefixAddress(), type == CONFIRM ? TransactionResult.Status.OPERATION_APPROVAL : TransactionResult.Status.OPERATION_REVOKE);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (isViewAttached()) {
                            getView().updateSigningStatus(individualWalletEntity.getPrefixAddress(), TransactionResult.Status.OPERATION_UNDETERMINED);
                            showLongToast(string(R.string.transfer_failed));
                        }
                    }
                });
    }

    private Map<String, String> buildTransactionInfo(String walletName) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(string(R.string.execute_wallet), walletName);
        map.put(string(R.string.type), string(R.string.executeContractFee));
        return map;
    }
}
