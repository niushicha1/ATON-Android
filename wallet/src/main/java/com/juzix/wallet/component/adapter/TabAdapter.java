package com.juzix.wallet.component.adapter;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.juzix.wallet.component.ui.base.BaseFragment;

import java.util.ArrayList;

public class TabAdapter extends FragmentStatePagerAdapter {

    private ArrayList<BaseFragment> mFragments;
    private ArrayList<String>       mTitles;

    public TabAdapter(FragmentManager fm, ArrayList<String> mTitles, ArrayList<BaseFragment> fragments) {
        super(fm);
        this.mFragments = fragments;
        this.mTitles = mTitles;
    }

    public void destroyAll() {
        for (int i = 0; i < mFragments.size(); i++) {
            try {
                BaseFragment baseFragment = mFragments.get(i);
                baseFragment.onDestroyView();
                destroyItem(null, i, mFragments.get(i));
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        }
    }

    public ArrayList<BaseFragment> getFragments() {
        return mFragments;
    }

    public ArrayList<String> getTitles() {
        return mTitles;
    }

    @Override
    public BaseFragment getItem(int position) {
        if (mFragments == null || mFragments.isEmpty() || position >= getCount()) {
            return null;
        }
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles.get(position % mTitles.size());
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
    }
}
