package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.adapter;

import android.content.Context;
import android.view.View;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.R;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.bean.BlueDevice;

import java.util.List;

public class BluetoothRecyclerViewAdapter extends BaseRecyclerViewAdapter<BlueDevice> {
    private List<BlueDevice> mDevice;
    private Context context;
    private String[] mStateArray = {"未绑定", "绑定中", "已绑定", "已连接"};

    public BluetoothRecyclerViewAdapter(Context mContext, List<BlueDevice> mData, int layoutId) {
        super(mContext, mData, layoutId);
        this.context = mContext;
        this.mDevice = mData;
    }

    @Override
    public void covert(BaseViewHolder holder, BlueDevice item, final int position) {
        holder.setTextView(R.id.tv_blue_name, item.getName());
        holder.setTextView(R.id.tv_blue_address, item.getAddress());
        holder.setTextView(R.id.tv_blue_state, mStateArray[item.getState() - 10]);
        holder.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListenerInterface != null) {
                    onItemClickListenerInterface.onClick(position);
                }
            }
        });
    }

    private OnItemClickListenerInterface onItemClickListenerInterface;

    public void setOnItemClickListenerInterface(OnItemClickListenerInterface onItemClickListenerInterface) {
        this.onItemClickListenerInterface = onItemClickListenerInterface;
    }

    public interface OnItemClickListenerInterface {
        void onClick(int position);
    }
}
