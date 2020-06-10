package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.deviceA;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.BluetoothLeDeviceBase;

public class BluetoothLeDeviceA extends BluetoothLeDeviceBase {

    private static final String TAG = "BluetoothLeDeviceA";
    //根据具体硬件进行设置
    public static String DEVICEA_UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String DEVICEA_UUID_CHARACTERISTIC = "0000fff4-0000-1000-8000-00805f9b34fb";

    //一般不用修改
    public static String DEVICEA_UUID_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    public BluetoothLeDeviceA(Context context) {
        super(context);
        UUID_SERVICE = DEVICEA_UUID_SERVICE;
        UUID_CHARACTERISTIC = DEVICEA_UUID_CHARACTERISTIC;
        UUID_DESCRIPTOR = DEVICEA_UUID_DESCRIPTOR;
    }

    @Override
    public void discoverServicesSuccess() {
        if(onClickParseDataInterface!=null){
            onClickParseDataInterface.onDiscoverServiceSuccess();
        }
    }


    @Override
    public void parseData(byte[] value) {
        Log.e(TAG, "BluetoothLeDeviceA-parseData: -------解析数据");
        if(onClickParseDataInterface!=null){
            onClickParseDataInterface.onParseInterface(value);
        }
    }

    public void setOnClickParseDataInterface(OnClickParseDataInterface onClickParseDataInterface) {
        this.onClickParseDataInterface = onClickParseDataInterface;
    }

    private OnClickParseDataInterface onClickParseDataInterface;
    public interface OnClickParseDataInterface{
        void onParseInterface(byte[] value);
        void onDiscoverServiceSuccess();
    }
}
