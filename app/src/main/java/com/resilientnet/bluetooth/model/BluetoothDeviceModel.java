package com.resilientnet.bluetooth.model;

public class BluetoothDeviceModel {

    private final String name;
    private final String address;

    public BluetoothDeviceModel(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}