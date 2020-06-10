/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.callback.BleDevceScanCallback;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.callback.OnDeviceConnectChangedListener;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.callback.OnScanCallback;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.callback.OnWriteCallback;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.utils.HexUtil;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;


public abstract class BluetoothLeDeviceBase {
    private final static String TAG = BluetoothLeDeviceBase.class.getSimpleName();

    private Context context;

    //默认扫描时间：60s
    private static final int SCAN_TIME = 60000;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    /**
     * BluetoothGatt作为中央来使用和处理数据，通过BluetoothGatt可以连接设备（connect）,发现服务（discoverServices），
     * 并把相应地属性返回到BluetoothGattCallback，BluetoothGattCallback返回中央的状态和周边提供的数据。bluetoothGatt 作为中央来使用与处理数据
     * ，通过它可以进行连接
     * BluetoothGattDescriptor：可以看成是描述符，对Characteristic的描述，包括范围、计量单位等。
     *
     * BluetoothGattService：服务，Characteristic的集合。
     *  BluetoothProfile： 一个通用的规范，按照这个规范来收发数据。
     * BluetoothGattCallback：已经连接上设备，对设备的某些操作后返回的结果。这里必须提醒下，已经连接上设备后的才可以返回，没有返回的认真看看有没有连接上设备
     */
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private HashMap<String, Map<String, BluetoothGattCharacteristic>> servicesMap = new HashMap<>();
    private BluetoothGattCharacteristic mBleGattCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic1;

    protected String UUID_SERVICE = "";
    protected String UUID_CHARACTERISTIC = "";
    protected String UUID_DESCRIPTOR = "";

    private Handler handler = new Handler();
    private boolean mScanning;
    private BleDevceScanCallback bleDeviceScanCallback;
    private OnDeviceConnectChangedListener connectChangedListener;
    private BluetoothGattService gattService;
    private int indexType = 0;
    private byte[][] data = new byte[][]{{2, 0, 19, 67, 79, 49, 50, 51, 52, 53, 54, 55, 56, 1, 73, -33, 77, -19, -61, -1},
            {41, -45, -26, 3}};
    private byte[] data2 = new byte[]{2, 0, 19, 67, 79, 49, 50, 51, 52, 53, 54, 55, 56, 1, 73, -33, 77, -19, -61, -1, 41, -45, -26, 3};

    public BluetoothLeDeviceBase(Context context) {
        this.context = context;
        initialize();
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //连接状态发送改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (connectChangedListener != null) {
                    connectChangedListener.onConnected();
                }
                mConnectionState = STATE_CONNECTED;
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectChangedListener != null) {
                    connectChangedListener.onDisconnected();
                }
                mConnectionState = STATE_DISCONNECTED;
            }
        }

        //对设备进行回调，订阅，发现新服务，设备订阅会返回所有特征的服务集合，比如心率监视器的服务，这个服务包含一个心率测量特征
        //一个远程设备会对应很多个服务，这些服务又有特征集合，通过蓝牙来操作这些特征集合
        // 描述符（Descriptor）--描述符定义了属性用来描述特征值。比如，描述符可以是人们可读的描述语句，可以是特征值得可接受范围，或者是特征值的测量单位。
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                //获取远程设备的所有服务
//                List<BluetoothGattService> services = mBluetoothGatt.getServices();
//                //遍历所以服务
//                for (int i = 0; i < services.size(); i++) {
//                    HashMap<String, BluetoothGattCharacteristic> charMap = new HashMap<>();
//                    //BluetoothGattService 服务 BluetoothGattCharacteristic的集合
//                    BluetoothGattService bluetoothGattService = services.get(i);
//                    //根据服务来获取每个UUID的值，每个属性都是通过UUID来确定的
//                    String serviceUuid = bluetoothGattService.getUuid().toString();
//                    //获取服务的所有特征集合
//                    List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
//                    for (int j = 0; j < characteristics.size(); j++) {
//                        charMap.put(characteristics.get(j).getUuid().toString(), characteristics.get(j));
//                    }
//                    servicesMap.put(serviceUuid, charMap);
//                }
//                BluetoothGattCharacteristic bluetoothGattCharacteristic = getBluetoothGattCharacteristic(UUID_SERVICE, UUID_CHARACTERISTIC);
//                if (bluetoothGattCharacteristic == null)
//                    return;
//                enableGattServicesNotification(bluetoothGattCharacteristic);

                Log.e("mcy", "发现服务成功...");
                gattService = gatt.getService(UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"));
                indexType = 1;
                if (gattService == null) {
                    indexType = 2;
                    gattService = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
                }
                if (gattService == null) {
                    Log.e("mcy", "获取bluetoothGattService失败...");
                } else {
                    if (indexType == 1) {
                        mBleGattCharacteristic = gattService.getCharacteristic(UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3"));
                    } else {
                        mBleGattCharacteristic = gattService.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
                    }
                    if (mBleGattCharacteristic == null) {
                        Log.e("mcy", "获取Characteristic失败...");
                    } else {
                        //这一句是为了接受蓝牙数据,必须写!!!否则接受不到数据
                        mBluetoothGatt.setCharacteristicNotification(mBleGattCharacteristic, true);
                        discoverServicesSuccess();
                    }
                }

            } else {
                Log.w(TAG, " --------- onServicesDiscovered received: " + status);
            }
        }

        //读取从设备服务特征值中传过来的数据
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead: status---=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                parseData(value);
            }
        }

        //向远程蓝牙设备的特征中写入数据
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("--------write success----- status:" + status);
        }

        /*
         * 蓝牙返回的数据回调
         * 当订阅的Characteristic接收到消息时回调 连接成功回调
         * 当设备上某个特征发送改变的时候就需要通知APP，通过以下方法进行设置通知
         * 一旦接收到通知那么远程设备发生改变的时候就会回调 onCharacteristicChanged
         * when connected successfully will callback this method
         * this method can dealwith send password or data analyze
         *
         * */

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: -----------=");
            byte[] value = characteristic.getValue();
            parseData(value);
        }
    };

    public abstract void discoverServicesSuccess();


    public abstract void parseData(byte[] value);

    private Handler handler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            writeData();

        }
    };

    /**
     * 发送数据使用 handler 处理了一下，没有直接发送。因为Characteristic最长只能发送 20 个字节，如果要是超过 20 个字节，就得循环发送，
     * 当然了，接受数据的时候，也是这个样子，一次接收不完，循环接收。
     */
    public void sendToMsgOne() {
        //不写这句，蓝牙消息传不回来，向当前特征值进行发送数据，然后就会回调onCharacteristicChanged，来返回该特征传回的数据
        mBluetoothGatt.setCharacteristicNotification(mBleGattCharacteristic, true);
        if (mBluetoothGatt != null && mBleGattCharacteristic != null) {
            //设置读数据的UUID
            for (byte[] num : data) {
                mBleGattCharacteristic.setValue(num);
                Message message = handler1.obtainMessage();
                message.obj = num;
                handler1.sendMessage(message);
            }
        }
    }

    /**
     * 向远程蓝牙设备进行写入特征指令
     */
    private void writeData() {
        try {
            boolean b = mBluetoothGatt.writeCharacteristic(mBleGattCharacteristic);
            if (b) {
                Thread.sleep(200);
            } else {
                for (int i = 0; i < 10; i++) {
                    if (mBluetoothGatt.writeCharacteristic(mBleGattCharacteristic)) {
                        return;
                    }
                }
                Log.e("mcy", "10次递归发送数据失败" + b);
                close();
            }
            Log.e("mcy", "发送数据是否成功:" + b);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //存储待发送的数据队列
    public Queue<byte[]> dataInfoQueue = new LinkedList<>();

    private Handler handler2 = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            send();
        }
    };

    /**
     * 向蓝牙发送数据方式二
     */
    public void sendDataToBT2() {
        if (dataInfoQueue != null) {
            dataInfoQueue.clear();
            dataInfoQueue = Utils.splitPacketFor20Byte(data2);
            handler2.post(runnable);
        }

    }

    private void send() {
        if (dataInfoQueue != null && !dataInfoQueue.isEmpty()) {
            //检测到发送数据，直接发送
            if (dataInfoQueue.peek() != null) {
                mBleGattCharacteristic.setValue(dataInfoQueue.poll());//移除并返回队列头部的元素
                boolean b = mBluetoothGatt.writeCharacteristic(mBleGattCharacteristic);
                Log.e("mcy", "发送数据是否成功:" + b);
            }
            //检测还有数据，延时后继续发送，一般延时100毫秒左右
            if (dataInfoQueue.peek() != null) {
                handler2.postDelayed(runnable, 100);
            }
        }
    }


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public void initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, " --------- Unable to initialize BluetoothManager. --------- ");
                return;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, " --------- Unable to obtain a BluetoothAdapter. --------- ");
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, " --------- BluetoothAdapter not initialized or unspecified address. --------- ");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, " --------- Device not found.  Unable to connect. --------- ");
            return false;
        }


        mBluetoothGatt = device.connectGatt(context, true, mGattCallback);
        Log.d(TAG, " --------- Trying to create a new connection. --------- ");
        mConnectionState = STATE_CONNECTING;
        return true;
    }


//    /**
//     * 连接方式二
//     */
//
//    public void connectLeDevice(Context context, BluetoothDevice device) {
//        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
//    }
//    /**
//     * 连接方式一
//     */
//    public void connection(Context context, String address) {
//        if (BluetoothAdapter.checkBluetoothAddress(address)) {
//            BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(address);
//            if (remoteDevice == null) {
//                Log.e("mcy", "设备不可用");
//            }
//            connectLeDevice(context, remoteDevice);
//        } else {
//            Log.e("mcy", "设备不可用");
//        }
//    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, " --------- BluetoothAdapter not initialized --------- ");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, " --------- BluetoothAdapter not initialized --------- ");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, " --------- BluetoothAdapter not initialized --------- ");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (UUID_CHARACTERISTIC.equals(characteristic.getUuid().toString())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(UUID_DESCRIPTOR));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.d(TAG, " --------- Connect setCharacteristicNotification --------- " + characteristic.getUuid());
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }


    /**
     * 当前蓝牙是否打开
     */
    private boolean isEnable() {
        if (null != mBluetoothAdapter) {
            return mBluetoothAdapter.isEnabled();
        }
        return false;
    }


    /**
     * @param enable
     * @param scanCallback
     */
    public void scanBleDevice(final boolean enable, final OnScanCallback scanCallback) {
        scanBleDevice(SCAN_TIME, enable, scanCallback, null);
    }

    /**
     * @param enable
     * @param scanCallback
     * @param specificUUids 扫描指定service uuid的设备
     */
    public void scanBleDevice(final boolean enable, final OnScanCallback scanCallback, UUID[] specificUUids) {
        scanBleDevice(SCAN_TIME, enable, scanCallback, specificUUids);
    }

    /**
     * 扫描
     */
    public void scanLeDevice(final BluetoothAdapter.LeScanCallback leScanCallback, final ScanCallback scanCallback, OnScanCallback callback) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(scanCallback);//开始搜索
                if (callback != null) {
                    callback.onScanning();
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        //10s后停止扫描
                        mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
                        if (callback != null) {
                            callback.onFinish();
                        }
                    }
                }, SCAN_TIME);
            } else {
                Log.e("mcy", "蓝牙不可用...");
            }
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.startLeScan(leScanCallback); //开始搜索
                if (callback != null) {
                    callback.onScanning();
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
                        if (callback != null) {
                            callback.onFinish();
                        }
                    }
                }, SCAN_TIME);
            } else {
                Log.e("mcy", "蓝牙不可用...");
            }
        }
        Log.e("mcy", "开始扫描...");
    }

    /**
     * @param time          扫描时长
     * @param enable
     * @param scanCallback
     * @param specificUUids
     */
    public void scanBleDevice(int time, final boolean enable, final OnScanCallback scanCallback, UUID[] specificUUids) {
        if (!isEnable()) {
            mBluetoothAdapter.enable();
            Log.e(TAG, "Bluetooth is not open!");
        }
        if (null != mBluetoothGatt) {
            mBluetoothGatt.close();
        }
        if (bleDeviceScanCallback == null) {
            bleDeviceScanCallback = new BleDevceScanCallback(scanCallback);
        }
        if (enable) {
            if (mScanning) return;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //time后停止扫描
                    mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
                    scanCallback.onFinish();
                }
            }, time <= 0 ? SCAN_TIME : time);
            mScanning = true;
            if (specificUUids != null) {
                mBluetoothAdapter.startLeScan(specificUUids, bleDeviceScanCallback);
            } else {
                mBluetoothAdapter.startLeScan(bleDeviceScanCallback);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
        }
    }


    public void stopScan() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
    }


    public void writeBuffer(byte[] value, OnWriteCallback writeCallback) {
        writeBuffer(HexUtil.bytesToHexString(value), writeCallback);
    }

    /**
     * 发送数据
     *
     * @param value         指令
     * @param writeCallback 发送回调
     */
    public void writeBuffer(String value, OnWriteCallback writeCallback) {
        if (!isEnable()) {
            if (writeCallback != null) {
                writeCallback.onFailed(OnWriteCallback.FAILED_BLUETOOTH_DISABLE);
            }
            Log.e(TAG, "FAILED_BLUETOOTH_DISABLE");
            return;
        }

        if (mBleGattCharacteristic == null) {
            mBleGattCharacteristic = getBluetoothGattCharacteristic(UUID_SERVICE, UUID_CHARACTERISTIC);
        }

        if (null == mBleGattCharacteristic) {
            if (writeCallback != null) {
                writeCallback.onFailed(OnWriteCallback.FAILED_INVALID_CHARACTER);
            }
            Log.e(TAG, "FAILED_INVALID_CHARACTER");
            return;
        }

        //设置数组进去
        mBleGattCharacteristic.setValue(HexUtil.hexStringToBytes(value));
        //发送

        boolean b = mBluetoothGatt.writeCharacteristic(mBleGattCharacteristic);

        if (b) {
            if (writeCallback != null) {
                writeCallback.onSuccess();
            }
        }
        Log.e(TAG, "send:" + b + "data：" + value);
    }

    /**
     * 根据服务UUID和特征UUID,获取一个特征{@link BluetoothGattCharacteristic}
     *
     * @param serviceUUID   服务UUID
     * @param characterUUID 特征UUID
     */
    private BluetoothGattCharacteristic getBluetoothGattCharacteristic(String serviceUUID, String characterUUID) {
        if (!isEnable()) {
            throw new IllegalArgumentException(" Bluetooth is no enable please call BluetoothAdapter.enable()");
        }
        if (null == mBluetoothGatt) {
            Log.e(TAG, "mBluetoothGatt is null");
            return null;
        }

        //找服务
        Map<String, BluetoothGattCharacteristic> bluetoothGattCharacteristicMap = servicesMap.get(serviceUUID);
        if (null == bluetoothGattCharacteristicMap) {
            Log.e(TAG, "Not found the serviceUUID!");
            return null;
        }

        //找特征
        Set<Map.Entry<String, BluetoothGattCharacteristic>> entries = bluetoothGattCharacteristicMap.entrySet();
        BluetoothGattCharacteristic gattCharacteristic = null;
        for (Map.Entry<String, BluetoothGattCharacteristic> entry : entries) {
            if (characterUUID.equals(entry.getKey())) {
                gattCharacteristic = entry.getValue();
                break;
            }
        }
        return gattCharacteristic;
    }


    private void enableGattServicesNotification(BluetoothGattCharacteristic gattCharacteristic) {
        if (gattCharacteristic == null) return;
        setNotify(gattCharacteristic);
    }

    private void setNotify(BluetoothGattCharacteristic characteristic) {

        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic1 != null) {
                setCharacteristicNotification(
                        mNotifyCharacteristic1, false);
                mNotifyCharacteristic1 = null;
            }
            readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic1 = characteristic;
            setCharacteristicNotification(
                    characteristic, true);
        }
    }


    public BluetoothAdapter isDeviceSupport() {
        //需要设备支持ble
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return null;
        }

        //需要有BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter;
    }

    public BluetoothLeScanner getBluetoothLeScanner() {
        //需要设备支持ble
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return null;
        }

        //需要有BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            return null;
        }
        return scanner;
    }

    public void setConnectChangedListener(OnDeviceConnectChangedListener connectChangedListener) {
        this.connectChangedListener = connectChangedListener;
    }
}
