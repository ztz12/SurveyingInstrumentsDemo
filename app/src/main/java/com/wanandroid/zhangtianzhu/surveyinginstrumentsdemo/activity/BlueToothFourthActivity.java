package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.R;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.AssistStatic;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.BluetoothUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 蓝牙4.0 使用：相比经典蓝牙,蓝牙低功耗（BLE）可以显著减少电量消耗。这使得Android应用可以与对电量有着更严格限制的设备通信，
 * 比如位置传感器，心率监视器，以及健身设备。
 * 以下是对BLE关键术语和概念的总结：
 * Generic Attribute Profile (GATT)--GATT profile是发送和接收少量数据的一种通用规范，正如大家知道的在BLE连接上的“attributes”。当前所有的低功耗应用profile都是基于GATT。
 * 蓝牙技术联盟为低功耗设备定义了很多profile。profile就是指定设备在特定的应用中如何工作的一种规范 。举个例子，一个设备可以包含一个心率监视器和一个电量状态监测器。
 * Attribute Protocol (ATT)--GATT是建立在ATT的基础之上。所以也称之为GATT/ATT。ATT在BLE设备上运行经过了优化，因此，它会尽可能使用更少的字节数组。每个属性通过UUID来唯一确定，
 * UUID是一种128位的标准化格式字符串ID来标识信息的唯一性。属性会被格式化为特征（characteristics ）与服务（services）后通过ATT来传输。
 * 特征（Characteristic）--一个特征包含了一个单独的值和0-n个描述符（descriptors ，用来描述特征的值）。一个特征可以看做是一种类型，类似一个class。
 * 描述符（Descriptor）--描述符定义了属性用来描述特征值。比如，描述符可以是人们可读的描述语句，可以是特征值得可接受范围，或者是特征值的测量单位。
 * 服务（Service）--服务是特征的集合。比如，你有一个叫做‘心率监视器’的服务，在这个服务里包含一个“心率测量”的特征。你可以在bluetooth.org上查看一系列基于GATT的profile与服务。
 */
public class BlueToothFourthActivity extends BaseActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private static final String TAG = "BlueToothFourthActivity";
    private BluetoothGatt bleGatt;
    private List<BluetoothGattCharacteristic> characteristics;
    private List<BluetoothGattDescriptor> descriptors;
    private List<BluetoothGattService> services;
    private Handler mHandler = new Handler();
    private int mOpenCode = -1;


    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    @Override
    public int getLayoutId() {
        return R.layout.activity_blut_tooth_fourth;
    }

    @Override
    public void initViews() {

    }

    @Override
    public void initData() {
        initBlueTooth();
    }

    /**
     * 初始化蓝牙适配器
     */
    private void initBlueTooth() {
        // Android从4.3开始增加支持BLE技术（即蓝牙4.0及以上版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //从系统服务中获取蓝牙管理器
            BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager != null) {
                mBluetoothAdapter = manager.getAdapter();
            }
        } else {
            //获取系统默认的蓝牙适配器
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (mBluetoothAdapter == null) {
            AssistStatic.showToast(this, "未找到蓝牙");
        }
        if (!isCheck()) {
            BluetoothUtil.setBlueToothStatus(this, true);
            mHandler.post(discoverRunnable);
        }
    }

    private boolean isCheck() {
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && !mBluetoothAdapter.isDiscovering());
    }

    private Runnable discoverRunnable = new Runnable() {
        @Override
        public void run() {
            //android 8.0要在已经打开蓝牙功能才会弹出下面弹窗
            if (BluetoothUtil.getBlueToothStatus(BlueToothFourthActivity.this)) {
                //弹出是否允许扫描蓝牙设备的弹窗
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                //设置检测蓝牙时间，默认为120s，通过以下方式设置为100s
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 100);
                startActivityForResult(intent, mOpenCode);
            } else {
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //允许蓝牙扫描对话框
        if (requestCode == mOpenCode) {
            //延迟50毫秒启动刷新蓝牙任务
            mHandler.postDelayed(mRefresh, 50);
            if (resultCode == RESULT_OK) {
                AssistStatic.showToast(this, "允许本地蓝牙被附近的其它蓝牙设备发现");
            } else if (resultCode == RESULT_CANCELED) {
                AssistStatic.showToast(this, "不允许蓝牙被附近的其它蓝牙设备发现");
            }
        }
    }

    /**
     * 定义一个刷新任务，每隔两秒刷新扫描到的蓝牙设备
     */
    private Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            if(!mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.startDiscovery();
            }; // 开始扫描周围的蓝牙设备
            // 延迟2秒后再次启动蓝牙设备的刷新任务
//            mHandler.postDelayed(this, 2000);
        }
    };

    // 连接蓝牙设备，device为之前扫描得到的
    public void connect(BluetoothDevice device) {
        if (bleGatt.connect()) {  // 已经连接了其他设备
            // 如果是先前连接的设备，则不做处理
            if (TextUtils.equals(device.getAddress(), bleGatt.getDevice().getAddress())) {
                return;
            } else {
                disconnect();  // 否则断开连接
            }
        }
        // 连接设备，第二个参数为是否自动连接，第三个为回调函数
        bleGatt = device.connectGatt(this, false, bleGattCallback);
    }

    // 实现连接回调接口[关键]
    private BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {

        // 连接状态改变(连接成功或失败)时回调该接口
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {   // 连接成功
                Intent intent = new Intent();
                sendBroadcast(intent);    // 这里是通过广播通知连接成功，依各自的需求决定
                gatt.discoverServices();   // 则去搜索设备的服务(Service)和服务对应Characteristic
            } else if(newState == BluetoothGatt.STATE_DISCONNECTED){   // 连接失败
                Intent intent = new Intent();
                sendBroadcast(intent);
            }
        }

        // 发现设备的服务(Service)回调，需要在这里处理订阅事件。
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {  // 成功订阅
                services = gatt.getServices();  // 获得设备所有的服务
                characteristics = new ArrayList<>();
                descriptors = new ArrayList<>();

                // 依次遍历每个服务获得相应的Characteristic集合
                // 之后遍历Characteristic获得相应的Descriptor集合
                for (BluetoothGattService service : services) {
                    Log.e(TAG, "-- service uuid : " + service.getUuid().toString() + " --");
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        Log.e(TAG, "characteristic uuid : " + characteristic.getUuid());
                        characteristics.add(characteristic);

                        String READ_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
                        // 判断当前的Characteristic是否想要订阅的，这个要依据各自蓝牙模块的协议而定
                        if (characteristic.getUuid().toString().equals(READ_UUID)) {
                            // 依据协议订阅相关信息,否则接收不到数据
                            bleGatt.setCharacteristicNotification(characteristic, true);

                            // 大坑，适配某些手机！！！比如小米...
                            bleGatt.readCharacteristic(characteristic);
                            for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                bleGatt.writeDescriptor(descriptor);
                            }
                        }
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            Log.e(TAG, "descriptor uuid : " + characteristic.getUuid());
                            descriptors.add(descriptor);
                        }
                    }
                }
                Intent intent = new Intent();
                sendBroadcast(intent);
            } else {
                Intent intent = new Intent();
                sendBroadcast(intent);
            }
        }

        // 发送消息结果回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            Intent intent;
            if (BluetoothGatt.GATT_SUCCESS == status) {   // 发送成功
                intent = new Intent();
            } else {    // 发送失败
                intent = new Intent();
            }
            sendBroadcast(intent);
        }

        // 当订阅的Characteristic接收到消息时回调
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // 数据为 characteristic.getValue())
            Log.e(TAG, "onCharacteristicChanged: " + Arrays.toString(characteristic.getValue()));
        }

    };

    private void disconnect() {
        if (bleGatt != null) {
            bleGatt.disconnect();
        }
    }
}
