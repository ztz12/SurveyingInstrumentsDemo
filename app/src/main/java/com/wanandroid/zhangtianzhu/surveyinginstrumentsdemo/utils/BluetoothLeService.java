package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.DeviceControlActivity;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    public final static String ACTION_GATT_CONNECTED = "com.choicemmed.bledemo.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.choicemmed.bledemo.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.choicemmed.bledemo.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.choicemmed.bledemo.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.choicemmed.bledemo.EXTRA_DATA";

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
//	                                                        00002902-0000-1000-8000-00805f9b34fb
//    public static String CLIENT_CHARACTERISTIC_CONFIG = "c7a7d8a5-eba8-4cc3-a5f2-53ca57921ec7";

    // Implements callback methods for GATT events that the app cares about. For
    // example,
    // connection change and services discovered.
    /**
     * 写入成功后，开始读取设备返回的数据
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //连接状态改变，连接成功或者失败
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:"
                        + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        //发现设备的服务回调，需要进行订阅事件
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        //从特征找那个读取数据
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        //向特征中写入数据
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("--------write success----- status:" + status);
        }

        ;

        /*
         * 当订阅的Characteristic接收到消息时回调 连接成功回调
         * 当设备上某个特征发送改变的时候就需要通知APP，通过以下方法进行设置通知
         * 一旦接收到通知那么远程设备发生改变的时候就会回调 onCharacteristicChanged
         * when connected successfully will callback this method
         * this method can dealwith send password or data analyze
         *
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getCharacteristic().getUuid();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            System.out.println("rssi = " + rssi);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        byte[] heartRate = characteristic.getValue();
        String s = DeviceControlActivity.byte2HexStr(heartRate);
        String data = DeviceControlActivity.print10(s);
        if (data != null) {
            intent.putExtra(EXTRA_DATA, data);
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * 开始连接设备
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "没有设备");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * 断开连接
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 一旦你的应用结束使用BLE设备，你就应该调用close方法来释放系统资源：
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 获取特征之后，找到服务中可以向手机写入指令的特征，向该特征写入指令
     *
     * @param characteristic
     */
    public void wirteCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);

    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "readCharacteristic: " + characteristic.getProperties());
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter为空");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 接收GATT通知
     * 当设备上某个特征发送改变的时候就需要通知APP，通过以下方法进行设置通知
     * 一旦接收到通知那么远程设备发生改变的时候就会回调 onCharacteristicChanged
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter为空");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID
                .fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Read the RSSI for a connected remote device.
     */
    public boolean getRssiVal() {
        if (mBluetoothGatt == null)
            return false;

        return mBluetoothGatt.readRemoteRssi();
    }
}
