package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.R;
import com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.activity.bluefourth.BlueToothFourthScanActivity;

public class StartPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);
    }

    public void btn_bluttoooth_fourth(View view) {
        startActivity(new Intent(this, BlueToothFourthScanActivity.class));
    }

    public void btn_bluetooth(View view) {
        startActivity(new Intent(this,MainActivity.class));
    }
}
