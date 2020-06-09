package com.wanandroid.zhangtianzhu.surveyinginstrumentsdemo.bean;

public class BlueDevice {
    public String name; // 蓝牙设备的名称
    public String address; // 蓝牙设备的MAC地址
    public int state; // 蓝牙设备的绑定状态

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
