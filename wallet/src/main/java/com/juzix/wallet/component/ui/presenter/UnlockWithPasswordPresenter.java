package com.juzix.wallet.component.ui.presenter;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import com.juzix.biometric.BiometricPromptCompat;
import com.juzix.wallet.R;
import com.juzix.wallet.component.ui.base.BaseActivity;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.UnlockWithPasswordContract;
import com.juzix.wallet.config.AppSettings;
import com.juzix.wallet.engine.IndividualWalletManager;
import com.juzix.wallet.entity.IndividualWalletEntity;

import java.util.ArrayList;

/**
 * @author matrixelement
 */
public class UnlockWithPasswordPresenter extends BasePresenter<UnlockWithPasswordContract.View> implements UnlockWithPasswordContract.Presenter{

    private final static String TAG = UnlockWithPasswordPresenter.class.getSimpleName();
    private IndividualWalletEntity mWallet;

    public UnlockWithPasswordPresenter(UnlockWithPasswordContract.View view) {
        super(view);
    }

    @Override
    public void setSelectWallet(IndividualWalletEntity wallet) {
        mWallet = wallet;
        getView().updateWalletInfo(wallet);
    }

    @Override
    public IndividualWalletEntity getSelectedWallet() {
        return mWallet;
    }

    @Override
    public void init() {
        mWallet = IndividualWalletManager.getInstance().getWalletList().get(0);
        setSelectWallet(mWallet);
    }


    @Override
    public void unlock(String password) {
        showLoadingDialog();
        new Thread(){
            @Override
            public void run() {
                if (!IndividualWalletManager.getInstance().isValidWallet(mWallet, password)){
                    mHandler.sendEmptyMessage(MSG_PASSWORD_FAILED);
                    return;
                } else {
                    mHandler.sendEmptyMessage(MSG_OK);
                }
            }
        }.start();
    }

    private static final int MSG_PASSWORD_FAILED = -1;
    private static final int MSG_OK = 1;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_PASSWORD_FAILED:
                    dismissLoadingDialogImmediately();
                    showLongToast(string(R.string.validPasswordError));
                    break;
                case MSG_OK:
                    if (!BiometricPromptCompat.supportBiometricPromptCompat(currentActivity())) {
                        AppSettings.getInstance().setFaceTouchIdFlag(false);
                    }
                    dismissLoadingDialogImmediately();
                    BaseActivity activity = currentActivity();
                    activity.setResult(Activity.RESULT_OK);
                    activity.finish();
                    break;
            }
        }
    };
}
