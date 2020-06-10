package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.R;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.MainActivity;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.callback.OnDeviceConnectChangedListener;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.callback.OnScanCallback;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.callback.OnWriteCallback;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.deviceA.BluetoothLeDeviceA;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.adapter.BluetoothRecyclerViewAdapter;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.bean.BlueDevice;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.BluetoothUtil;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 蓝牙4.0 低功耗蓝牙
 */
public class BlueToothFourthScanActivity extends AppCompatActivity {

    private static final String TAG = "UaTestActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private TextView mConnectionState1;
    private TextView mDataField;
    private TextView scanResult;

    private TextView send;
    private EditText sendCommand;
    private BluetoothLeDeviceA bluetoothLeDeviceA;
    private BluetoothRecyclerViewAdapter mAdapter;
    private RecyclerView mRl;
    private List<BlueDevice> mBlueDeviceList;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private ScanCallback scanCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth_fourth_scan);

        bluetoothLeDeviceA = new BluetoothLeDeviceA(this);
        mBlueDeviceList = new ArrayList<>();
        mRl = findViewById(R.id.rl_bluetooth);
        bluetoothLeDeviceA.setConnectChangedListener(new OnDeviceConnectChangedListener() {
            @Override
            public void onConnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mConnectionState1.setText("onConnected");
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mConnectionState1.setText("onDisconnected");
                    }
                });
            }
        });

        mBluetoothAdapter = bluetoothLeDeviceA.isDeviceSupport();
        mBluetoothLeScanner = bluetoothLeDeviceA.getBluetoothLeScanner();
        if (mBluetoothAdapter == null) {
            finish();
            return;
        }
        initBle();

        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });
        mConnectionState1 = findViewById(R.id.connection_state1);
        mDataField = findViewById(R.id.data_value);
        scanResult = findViewById(R.id.scan_result);
        sendCommand = findViewById(R.id.send_command);
        send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendContent = sendCommand.getText().toString();
                if (!TextUtils.isEmpty(sendContent)) {
                    bluetoothLeDeviceA.writeBuffer(sendContent, new OnWriteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.e(TAG, "write data success:");
                        }

                        @Override
                        public void onFailed(int state) {
                            Log.e(TAG, "write data faile-----" + state);
                        }
                    });
                } else {
                    Toast.makeText(BlueToothFourthScanActivity.this, "no content", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    /**
     * 初始化蓝牙列表
     */
    private void initBlueDevice() {
        //获取已经配对的蓝牙集合
        for (BluetoothDevice blueDevice : devices) {
            BlueDevice device = new BlueDevice();
            device.setName(blueDevice.getName());
            device.setAddress(blueDevice.getAddress());
            device.setState(blueDevice.getBondState());
            mBlueDeviceList.add(device);
        }
        if (mAdapter == null) {
            LinearLayoutManager manager = new LinearLayoutManager(this);
            mAdapter = new BluetoothRecyclerViewAdapter(this, mBlueDeviceList, R.layout.item_bluetooth);
            mRl.setLayoutManager(manager);
            mRl.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }

        mAdapter.setOnItemClickListenerInterface(new BluetoothRecyclerViewAdapter.OnItemClickListenerInterface() {
            @Override
            public void onClick(int position) {

                BlueDevice device = mBlueDeviceList.get(position);
                //根据设备地址获取远程蓝牙设备对象
                BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(device.getAddress());
                //低功耗蓝牙没有配对，直接连接
                //连接方式有两种，一种根据蓝牙地址获取远程设备进行连接，另一种直接连接，传入一个回调接口，反馈连接状态，根据状态进行下一步操作
                bluetoothLeDeviceA.connect(bluetoothDevice.getAddress());
            }
        });
    }

    /**
     * 刷新蓝牙列表
     */
    private void refreshDevice(BluetoothDevice device, int state) {
        int i;
        for (i = 0; i < mBlueDeviceList.size(); i++) {
            BlueDevice item = mBlueDeviceList.get(i);
            if (item.address.equals(device.getAddress())) {
                item.state = state;
                mBlueDeviceList.set(i, item);
                break;
            }
        }
        if (i >= mBlueDeviceList.size()) {
            BlueDevice blueDevice = new BlueDevice();
            blueDevice.setName(device.getName());
            blueDevice.setAddress(device.getAddress());
            blueDevice.setState(device.getBondState());
            mBlueDeviceList.add(blueDevice);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        scanLeDevice(true);
    }


    @Override
    protected void onPause() {
        super.onPause();
//        scanLeDevice(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothLeDeviceA.close();
    }

    ArrayList<BluetoothDevice> devices = new ArrayList<>();

    private void scanLeDevice(final boolean enable) {
        startScan(enable);
    }

    private void startScan(boolean enable) {
//        bluetoothLeDeviceA.scanBleDevice(enable, new OnScanCallback() {
//            @Override
//            public void onFinish() {
//                scanResult.setText("扫描成功");
//                //这里可以用ListView将扫描到的设备展示出来，然后选择某一个进行连接
////                 bluetoothLeDeviceA.connect("00:EC:0A:53:6C:EB");
////                StringBuilder stringBuilder = new StringBuilder();
////                stringBuilder.append("扫描成功：\n");
////                for (BluetoothDevice device : devices) {
////                    stringBuilder.append(device.getName());
////                    stringBuilder.append(",");
////                    stringBuilder.append(device.getAddress());
////                    stringBuilder.append("\n");
////                }
////                scanResult.setText(stringBuilder.toString());
//                initBlueDevice();
//            }
//
//            @Override
//            public void onScanning(final BluetoothDevice device, int rssi, byte[] scanRecord) {
//                if (!devices.contains(device)) {
//                    devices.add(device);
//                    scanResult.setText("扫描中........");
//                    Log.e(TAG, "onScanning: " + device.getAddress());
//                }
//            }
//        });
        bluetoothLeDeviceA.scanLeDevice(leScanCallback, scanCallback, new OnScanCallback() {
            @Override
            public void onFinish() {
                scanResult.setText("扫描成功");
                initBlueDevice();
            }

            @Override
            public void onScanning() {
                scanResult.setText("扫描中........（60s时间）");

            }
        });
    }

    private void initBle() {
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                if (!devices.contains(device)) {
                    devices.add(device);
                    Log.e("mcy", "扫描到设备-->" + device.getName());
                }
//                if (device.getName().equals("00doos009000012147")) {//连接制定的设备。！！！！！测试使用！！！！！！
//                    Log.e("mcy", "扫描到设备-->" + device.getName());
//                    stopScan(leScanCallback, scanCallback);
//                    bluetoothLeDeviceA.connect(device.getAddress());
//                }


            }
        };
        //api>21回调这个借口
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (!devices.contains(result.getDevice())) {
                    devices.add(result.getDevice());
                }
//                if (result.getDevice().getName().equals("00doos009000012147")) {//连接制定的设备。！！！！！测试使用！！！！！！
//                    Log.e("mcy", "扫描到设备-->" + result.getDevice().getName());
//                    stopScan(leScanCallback, scanCallback);
//                    bluetoothLeDeviceA.connect(result.getDevice().getAddress());
//                }
            }
        };
        bluetoothLeDeviceA.setOnClickParseDataInterface(new BluetoothLeDeviceA.OnClickParseDataInterface() {
            @Override
            public void onParseInterface(byte[] value) {
                stopScan(leScanCallback, scanCallback);
                StringBuilder stringBuilder = new StringBuilder();
                for (byte byteChar : value) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }
                String returnedPacket = stringBuilder.toString().replace(" ", "");
                byte[] packetByte = Utils.hexStringToByteArray(returnedPacket);
                if (packetByte.length - 5 == Utils.getLengthFromToken(packetByte)) {
                    Log.e("mcy_returnedPacket", returnedPacket);
                    mDataField.setText(returnedPacket);
                    bluetoothLeDeviceA.close();//取消连接
                }
            }

            @Override
            public void onDiscoverServiceSuccess() {
                //发现服务，进行发送数据
                stopScan(leScanCallback, scanCallback);
                bluetoothLeDeviceA.sendToMsgOne();
            }
        });
    }

    /**
     * 停止扫描
     */
    public void stopScan(BluetoothAdapter.LeScanCallback mLeScanCallback, ScanCallback scanCallback) {
        Log.e("mcy", "停止扫描...");
        if (mBluetoothAdapter != null && mLeScanCallback != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        if (mBluetoothLeScanner != null && scanCallback != null) {
            mBluetoothLeScanner.stopScan(scanCallback);
        }

    }
}
