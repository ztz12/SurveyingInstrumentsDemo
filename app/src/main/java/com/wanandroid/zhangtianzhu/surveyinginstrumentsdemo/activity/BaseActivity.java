package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;


import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.utils.LocalUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {
    private final int REQUEST_PERMISSION_ACCESS_LOCATION = 0;
    private List<String> permissions = new ArrayList<>();
    private int mPermissionRequestCount = 0;
    private int MAX_NUMBER_REQUEST_PERMISSIONS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        initViews();
        requestPermissionIfNecessary();
        initData();
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
                if (permissionUseInterface != null) {
                    permissionUseInterface.onPermissionUse();
                }
            }
        } else {
            if (permissionUseInterface != null) {
                permissionUseInterface.onPermissionUse();
            }
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
                if (LocalUtils.checkGPSIsOpen(this)) {
                    if (checkAllPermission()) {
                        if (permissionUseInterface != null) {
                            permissionUseInterface.onPermissionUse();
                        }
                    } else {
                        showSettingDialog();
                    }
                } else {
                    LocalUtils.goToOpenGPS(this);
                }
            }
            break;
            default:
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

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
                intent.setData(Uri.parse("package:" + BaseActivity.this.getPackageName()));
                startActivity(intent);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private OnPermissionUseInterface permissionUseInterface;

    public void setPermissionUseInterface(OnPermissionUseInterface permissionUseInterface) {
        this.permissionUseInterface = permissionUseInterface;
    }

    public interface OnPermissionUseInterface {
        void onPermissionUse();
    }

    public abstract int getLayoutId();

    public abstract void initViews();

    public abstract void initData();
}
