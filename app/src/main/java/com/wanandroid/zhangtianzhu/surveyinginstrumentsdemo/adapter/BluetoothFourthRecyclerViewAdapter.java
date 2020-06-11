package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.R;

import java.util.List;

public class BluetoothFourthRecyclerViewAdapter extends BaseRecyclerViewAdapter<BluetoothDevice> {
    private Context context;

    public BluetoothFourthRecyclerViewAdapter(Context mContext, List<BluetoothDevice> mData, int layoutId){
        super(mContext, mData, layoutId);
        this.context = mContext;
    }

    @Override
    public void covert(BaseViewHolder holder, BluetoothDevice item, final int position) {
        holder.setTextView(R.id.tv_blue_name, item.getName());
        holder.setTextView(R.id.tv_blue_address, item.getAddress());
        holder.setVisibility(R.id.tv_blue_state, View.GONE);

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
