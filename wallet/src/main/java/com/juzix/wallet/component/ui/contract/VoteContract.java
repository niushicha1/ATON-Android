package com.juzix.wallet.component.ui.contract;

import com.juzix.wallet.component.ui.SortType;
import com.juzix.wallet.component.ui.base.IPresenter;
import com.juzix.wallet.component.ui.base.IView;
import com.juzix.wallet.entity.CandidateEntity;
import com.juzix.wallet.entity.IndividualWalletEntity;

import java.util.List;

/**
 * @author matrixelement
 */
public class VoteContract {

    public interface View extends IView {

        void setVotedInfo(long sumVoteNum, long votedNum, String ticketPrice);

        void notifyDataSetChanged(List<CandidateEntity> candidateList);
    }

    public interface Presenter extends IPresenter<View> {

        void start();

        void sort(SortType sortType);

        void search(String keyword);

        void voteTicket(CandidateEntity candidateEntity);
    }
}
