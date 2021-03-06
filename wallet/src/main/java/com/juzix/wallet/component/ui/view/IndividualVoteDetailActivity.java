package com.juzix.wallet.component.ui.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juzhen.framework.util.NumberParserUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.app.Constants;
import com.juzix.wallet.component.ui.base.MVPBaseActivity;
import com.juzix.wallet.component.ui.contract.IndividualVoteDetailContract;
import com.juzix.wallet.component.ui.presenter.IndividualVoteDetailPresenter;
import com.juzix.wallet.entity.SingleVoteEntity;
import com.juzix.wallet.utils.BigDecimalUtil;
import com.juzix.wallet.utils.CommonUtil;
import com.juzix.wallet.utils.DateUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * @author matrixelement
 */
public class IndividualVoteDetailActivity extends MVPBaseActivity<IndividualVoteDetailPresenter> implements IndividualVoteDetailContract.View {

    @BindView(R.id.iv_copy_from_address)
    ImageView ivCopyFromAddress;
    @BindView(R.id.tv_from_address)
    TextView tvFromAddress;
    @BindView(R.id.layout_from_address)
    RelativeLayout layoutFromAddress;
    @BindView(R.id.iv_copy_to_address)
    ImageView ivCopyToAddress;
    @BindView(R.id.tv_to_address)
    TextView tvToAddress;
    @BindView(R.id.layout_to_address)
    RelativeLayout layoutToAddress;
    @BindView(R.id.tv_transaction_type_title)
    TextView tvTransactionTypeTitle;
    @BindView(R.id.tv_transaction_time_title)
    TextView tvTransactionTimeTitle;
    @BindView(R.id.tv_transaction_node_name_title)
    TextView tvTransactionNodeNameTitle;
    @BindView(R.id.tv_transaction_energon_title)
    TextView tvTransactionEnergonTitle;
    @BindView(R.id.tv_transaction_type)
    TextView tvTransactionType;
    @BindView(R.id.tv_transaction_time)
    TextView tvTransactionTime;
    @BindView(R.id.tv_transaction_node_name)
    TextView tvTransactionNodeName;
    @BindView(R.id.tv_transaction_node_id)
    TextView tvTransactionNodeId;
    @BindView(R.id.tv_transaction_votes)
    TextView tvTransactionVotes;
    @BindView(R.id.tv_transaction_ticket_price)
    TextView tvTransactionTicketPrice;
    @BindView(R.id.tv_transaction_staked)
    TextView tvTransactionStaked;
    @BindView(R.id.tv_transaction_energon)
    TextView tvTransactionEnergon;
    @BindView(R.id.iv_failed)
    ImageView ivFailed;
    @BindView(R.id.iv_succeed)
    ImageView ivSucceed;
    @BindView(R.id.layout_pending)
    RelativeLayout layoutPending;
    @BindView(R.id.tv_transaction_status_desc)
    TextView tvTransactionStatusDesc;
    @BindView(R.id.tv_transaction_node_id_title)
    TextView tvTransactionNodeIdTitle;
    @BindView(R.id.tv_transaction_votes_title)
    TextView tvTransactionVotesTitle;
    @BindView(R.id.tv_transaction_ticket_price_title)
    TextView tvTransactionTicketPriceTitle;
    @BindView(R.id.tv_transaction_staked_title)
    TextView tvTransactionStakedTitle;
    private Unbinder unbinder;

    @Override
    protected IndividualVoteDetailPresenter createPresenter() {
        return new IndividualVoteDetailPresenter(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_vote_detail);
        unbinder = ButterKnife.bind(this);
        mPresenter.fetchTransactionDetail();
    }

    @OnClick({R.id.iv_copy_from_address, R.id.iv_copy_to_address})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_copy_from_address:
                CommonUtil.copyTextToClipboard(this, tvFromAddress.getText().toString());
                break;
            case R.id.iv_copy_to_address:
                CommonUtil.copyTextToClipboard(this, tvToAddress.getText().toString());
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    public static void actionStart(Context context, String uuid) {
        Intent intent = new Intent(context, IndividualVoteDetailActivity.class);
        intent.putExtra(Constants.Extra.EXTRA_ID, uuid);
        context.startActivity(intent);
    }

    @Override
    public String getTransactionUuidFromIntent() {
        return getIntent().getStringExtra(Constants.Extra.EXTRA_ID);
    }

    @Override
    public void setTransactionDetailInfo(SingleVoteEntity voteEntity) {

        showTransactionStatus(voteEntity.getStatus());

        tvFromAddress.setText(voteEntity.getPrefixWalletAddress());
        tvToAddress.setText(voteEntity.getPrefixContractAddress());
        tvTransactionType.setText(R.string.vote);
        tvTransactionTime.setText(DateUtil.format(voteEntity.getCreateTime(), DateUtil.DATETIME_FORMAT_PATTERN));
        tvTransactionNodeName.setText(voteEntity.getCandidateName());
        tvTransactionNodeId.setText(voteEntity.getCandidateId());
        tvTransactionVotes.setText(String.valueOf(voteEntity.getTicketNumber()));
        tvTransactionTicketPrice.setText(string(R.string.amount_with_unit, NumberParserUtils.getPrettyBalance(BigDecimalUtil.div(Double.parseDouble(voteEntity.getTicketPrice()), 1E18))));
        tvTransactionStaked.setText(string(R.string.amount_with_unit, NumberParserUtils.getPrettyBalance(voteEntity.getValue())));
        tvTransactionEnergon.setText(string(R.string.amount_with_unit, NumberParserUtils.getPrettyBalance(voteEntity.getEnergonPrice())));
    }

    private void showTransactionStatus(int status) {
        switch (status) {
            case SingleVoteEntity.STATUS_PENDING:
                tvTransactionStatusDesc.setText(R.string.pending);
                ivFailed.setVisibility(View.GONE);
                ivSucceed.setVisibility(View.GONE);
                layoutPending.setVisibility(View.VISIBLE);
                break;
            case SingleVoteEntity.STATUS_SUCCESS:
                tvTransactionStatusDesc.setText(R.string.success);
                ivFailed.setVisibility(View.GONE);
                ivSucceed.setVisibility(View.VISIBLE);
                layoutPending.setVisibility(View.GONE);
                break;
            case SingleVoteEntity.STATUS_FAILED:
                tvTransactionStatusDesc.setText(R.string.failed);
                ivFailed.setVisibility(View.VISIBLE);
                ivSucceed.setVisibility(View.GONE);
                layoutPending.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }
}
