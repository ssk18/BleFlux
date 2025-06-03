package com.ssk.bleflux.data.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import com.ssk.bleflux.domain.connection.BleDevice
import com.ssk.bleflux.domain.connection.BleGattProvider

class BleGattProviderImpl : BleGattProvider {
    
    override fun connectGatt(
        context: Context,
        device: BleDevice,
        autoConnect: Boolean,
        callback: BluetoothGattCallback
    ): BluetoothGatt? {
        return try {
            device.bluetoothDevice.connectGatt(context, autoConnect, callback)
        } catch (e: SecurityException) {
            null
        }
    }
    
    override fun closeGatt(gatt: BluetoothGatt) {
        try {
            gatt.close()
        } catch (e: Exception) {
            // Ignore errors when closing
        }
    }
    
    override fun disconnectGatt(gatt: BluetoothGatt) {
        try {
            gatt.disconnect()
        } catch (e: SecurityException) {
            // Ignore security errors when disconnecting
        }
    }
}