package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.R;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.adapter.BluetoothRecyclerViewAdapter;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.bean.BlueDevice;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.AssistStatic;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.BluetoothUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "MainActivity";
    private final int REQUEST_PERMISSION_ACCESS_LOCATION = 0;
    private List<String> permissions = new ArrayList<>();
    private int mPermissionRequestCount = 0;
    private int MAX_NUMBER_REQUEST_PERMISSIONS = 4;
    private CheckBox ck_bluetooth;
    private TextView tv_discovery;
    private TextView tvReceiveData;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothRecyclerViewAdapter mAdapter;
    private RecyclerView mRl;
    private List<BlueDevice> mBlueDeviceList;
    /**
     * 是否允许扫描蓝牙设备的选择对话框返回结果代码
     */
    private int mOpenCode = 1;
    /**
     * 定义客户端蓝牙socket
     */
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private Handler mHandler = new Handler();
    private EditText editText;
    private Button btn;
    //这条是蓝牙串口通用的UUID，不要更改
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String receiveData;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket bluetoothServerSocket;
    private final String NAME = "Bluetooth_Socket";
    private InputStream inputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBlueDeviceList = new ArrayList<>();
        initBlueTooth();
        ck_bluetooth = findViewById(R.id.ck_bluetooth);
        tv_discovery = findViewById(R.id.tv_discovery);
        mRl = findViewById(R.id.rl_bluetooth);
        editText = findViewById(R.id.et);
        btn = findViewById(R.id.btn_send);
        tvReceiveData = findViewById(R.id.tv_receive_data);
        ck_bluetooth.setOnCheckedChangeListener(this);
        if (BluetoothUtil.getBlueToothStatus(this)) {
            ck_bluetooth.setChecked(true);
        }
        requestPermissionIfNecessary();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = "客户端发送的数据为：" + editText.getText().toString();
                try {
                    outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        tvReceiveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getServerData();
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

    private void requestPermissionIfNecessary() {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 23) {
            if (!checkAllPermission()) {
                if (mPermissionRequestCount < MAX_NUMBER_REQUEST_PERMISSIONS) {
                    mPermissionRequestCount += 1;
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_ACCESS_LOCATION);
                } else {
                    Toast.makeText(this, "缺失权限", Toast.LENGTH_LONG).show();
                }
            } else {
                initBlueDevice(); // 初始化蓝牙设备列表
            }
        } else {
            initBlueDevice(); // 初始化蓝牙设备列表
        }
    }

    private boolean checkAllPermission() {
        boolean hasPermission = true;
        for (String permission : permissions) {
            hasPermission = hasPermission && ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return hasPermission;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_LOCATION: {
                if (checkAllPermission()) {
                    Log.d(TAG, "开启权限permission granted!");
                    //做下面该做的事
                    initBlueDevice(); // 初始化蓝牙设备列表
                } else {
                    showSettingDialog();
                }
            }
            break;
            default:
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 初始化蓝牙列表
     */
    private void initBlueDevice() {
        mBlueDeviceList.clear();
        //获取已经配对的蓝牙集合
        final Set<BluetoothDevice> blueDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice blueDevice : blueDevices) {
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
                if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    //尚未配对
                    boolean isSuccess = BluetoothUtil.createBond(bluetoothDevice);
                    //配对成功
                    if (isSuccess) {
                        connectDevice(bluetoothDevice);
                    }
                } else if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //移除配对信息，已经配对
                    boolean isSuccess = BluetoothUtil.removeBond(bluetoothDevice);
                    //移除配对成功
                    if (isSuccess) {
                        refreshDevice(bluetoothDevice, BluetoothDevice.BOND_NONE);
                    }
                }
            }
        });
    }

    private Runnable discoverRunnable = new Runnable() {
        @Override
        public void run() {
            //android 8.0要在已经打开蓝牙功能才会弹出下面弹窗
            if (BluetoothUtil.getBlueToothStatus(MainActivity.this)) {
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

    protected void showSettingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("帮助");
        builder.setMessage("当前应用缺少权限。\n \n 请点击 \"设置\"-\"权限\"-打开所需权限。");
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                startActivity(intent);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.ck_bluetooth) {
            //开启蓝牙
            if (isChecked) {
                ck_bluetooth.setText("蓝牙处于开启状态");
                if (!BluetoothUtil.getBlueToothStatus(this)) {
                    BluetoothUtil.setBlueToothStatus(this, true);
                }
                mHandler.post(discoverRunnable);
            } else {
                ck_bluetooth.setText("蓝牙处于关闭状态");
                cancelDiscovery();
                BluetoothUtil.setBlueToothStatus(this, false);
                initBlueDevice();
            }
        }
    }

    /**
     * 定义一个刷新任务，每隔两秒刷新扫描到的蓝牙设备
     */
    private Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            beginDiscovery(); // 开始扫描周围的蓝牙设备
            // 延迟2秒后再次启动蓝牙设备的刷新任务
            mHandler.postDelayed(this, 2000);
        }
    };

    /**
     * 开始扫描周围的蓝牙设备
     */
    private void beginDiscovery() {
        // 如果当前不是正在搜索，则开始新的搜索任务
        if (!mBluetoothAdapter.isDiscovering()) {
            initBlueDevice(); // 初始化蓝牙设备列表
            tv_discovery.setText("正在搜索蓝牙设备");
            mBluetoothAdapter.startDiscovery(); // 开始扫描周围的蓝牙设备
        }
    }

    /**
     * 取消蓝牙设备的搜索
     */
    private void cancelDiscovery() {
        mHandler.removeCallbacks(mRefresh);
        tv_discovery.setText("取消搜索蓝牙设备");
        // 当前正在搜索，则取消搜索任务
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery(); // 取消扫描周围的蓝牙设备
        }
    }

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

    @Override
    protected void onStart() {
        super.onStart();
        mHandler.postDelayed(mRefresh, 50);
        // 需要过滤多个动作，则调用IntentFilter对象的addAction添加新动作
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mDiscoverReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelDiscovery();
        unregisterReceiver(mDiscoverReceiver);
    }

    private BroadcastReceiver mDiscoverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //发现新的蓝牙设备
            if (Objects.equals(action, BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //将发现的设备加入到蓝牙设备列表中
                if (device != null) {
                    refreshDevice(device, device.getBondState());
                }
            } else if (Objects.equals(action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                mHandler.removeCallbacks(mRefresh); // 需要持续搜索就要注释这行
                tv_discovery.setText("蓝牙设备搜索完成");
            } else if (Objects.equals(action, BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {//配对状态变更
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                        tv_discovery.setText("正在配对" + device.getName());
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        tv_discovery.setText("完成配对" + device.getName());
                        mHandler.postDelayed(mRefresh, 50);
                    } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        tv_discovery.setText("取消配对" + device.getName());
                        refreshDevice(device, device.getBondState());
                    }
                }
            }
        }
    };

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

    private void connectDevice(BluetoothDevice bluetoothDevice) {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        try {
            //创建客户端蓝牙socket
            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            //开始进行蓝牙链接，如果没有配对则弹出提示框，提示配对
            bluetoothSocket.connect();
            //获得输出流，客户端指向服务端输出内容
            outputStream = bluetoothSocket.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                bluetoothSocket.close();
            } catch (IOException e2) {
                Log.e("error", "ON RESUME: Unable to close socket during connection failure", e2);
            }
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
                bluetoothServerSocket = serverSocket.accept();
                inputStream = bluetoothServerSocket.getInputStream();
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
                    tvReceiveData.setText(receiveData);
                    AssistStatic.showToast(MainActivity.this, String.valueOf(msg.obj));
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
}
