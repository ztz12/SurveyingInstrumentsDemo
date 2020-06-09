package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity;

import androidx.annotation.NonNull;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.R;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.bean.BlueDevice;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.AssistStatic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothServerSocketActivity extends BaseActivity {
    private static final String TAG = "BluetoothServerSocketAc";

    //这条是蓝牙串口通用的UUID，不要更改
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter;
    private TextView tvSocket;
    private String receiveData;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket bluetoothSocket;
    private final String NAME = "Bluetooth_Socket";
    private InputStream inputStream;
    private List<BlueDevice> mBlueDeviceList;
    private EditText etServerSocket;
    private Button btnServerSocket;
    /**
     * 定义客户端蓝牙socket
     */
    private BluetoothSocket blueClientToothSocket;
    private OutputStream outputStream;


    @Override
    public int getLayoutId() {
        return R.layout.activity_bluetooth_server_socket;
    }

    @Override
    public void initViews() {
        tvSocket = findViewById(R.id.tv_server_socket);
        etServerSocket = findViewById(R.id.et_server_socket);
        btnServerSocket = findViewById(R.id.btn_server_send_msg);
    }

    @Override
    public void initData() {
        mBlueDeviceList = new ArrayList<>();
        initBlueTooth();
        setPermissionUseInterface(new OnPermissionUseInterface() {
            @Override
            public void onPermissionUse() {
                getServerData(); // 初始化蓝牙设备列表
            }
        });
        //作为服务端的手机变成客户端并进行发送数据
        btnServerSocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getConnectDevice();
                connectDevice();
            }
        });
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
    }

    private void getServerData() {
        new AcceptThread().start();
    }

    private class AcceptThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                serverSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
                bluetoothSocket = serverSocket.accept();
                inputStream = bluetoothSocket.getInputStream();
                while (true) {
                    byte[] buffer = new byte[1024];
                    //数据存储在buffer中
                    int count = inputStream.read(buffer);
                    processBuffer(buffer, 1024, count);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    tvSocket.setText(receiveData);
                    AssistStatic.showToast(BluetoothServerSocketActivity.this, String.valueOf(msg.obj));
                    break;
                default:
                    break;
            }
        }
    };

    private void processBuffer(byte[] buff, int size, int count) {
        int length = 0;
        for (int i = 0; i < size; i++) {
            if (buff[i] > '\0') {
                length++;
            } else {
                break;
            }
        }

        byte[] newbuff = new byte[length];  //newbuff字节数组，用于存放真正接收到的数据

        for (int j = 0; j < length; j++) {
            newbuff[j] = buff[j];
        }

        receiveData = receiveData + new String(newbuff);
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = new String(buff, 0, count, StandardCharsets.UTF_8);
        handler.sendMessage(msg);  //发送消息:系统会自动调用handleMessage( )方法来处理消息

    }


    /**
     * 获取已经连接的设备
     */
    private void getConnectDevice() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Class<BluetoothAdapter> bluetoothAdapterClass = BluetoothAdapter.class;//得到BluetoothAdapter的Class对象
        try {//得到连接状态的方法
            Method method = bluetoothAdapterClass.getDeclaredMethod("getConnectionState", (Class[]) null);
            //打开权限
            method.setAccessible(true);
            int state = (int) method.invoke(adapter, (Object[]) null);

            if (state == BluetoothAdapter.STATE_CONNECTED) {
                Log.i("BLUETOOTH", "BluetoothAdapter.STATE_CONNECTED");
                //获取已经配对的设备
                Set<BluetoothDevice> devices = adapter.getBondedDevices();
                Log.i("BLUETOOTH", "devices:" + devices.size());

                for (BluetoothDevice device : devices) {
                    Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
                    method.setAccessible(true);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
                    //判断当前设备是否连接
                    if (isConnected) {
                        Log.i("BLUETOOTH", "connected:" + device.getName());
                        BlueDevice blueDevice = new BlueDevice();
                        blueDevice.setName(device.getName());
                        blueDevice.setAddress(device.getAddress());
                        blueDevice.setState(device.getBondState());
                        mBlueDeviceList.add(blueDevice);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectDevice() {
        try {
            BlueDevice device = mBlueDeviceList.get(0);
            //根据设备地址获取远程蓝牙设备对象
            BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(device.getAddress());
            //创建客户端蓝牙socket
            blueClientToothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            //开始进行蓝牙链接，如果没有配对则弹出提示框，提示配对
            blueClientToothSocket.connect();
            //获得输出流，客户端指向服务端输出内容
            outputStream = blueClientToothSocket.getOutputStream();
            String text = "客户端发送的数据为：" + etServerSocket.getText().toString();
            outputStream.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                bluetoothSocket.close();
            } catch (IOException e2) {
                Log.e("error", "ON RESUME: Unable to close socket during connection failure", e2);
            }
        }
    }
}
