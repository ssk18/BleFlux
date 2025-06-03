package com.ssk.bleflux.domain.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context

/**
 * Interface for providing GATT connections
 */
interface BleGattProvider {
    
    /**
     * Connect to a GATT server on the device
     * @param context Application context
     * @param device The device to connect to
     * @param autoConnect Whether to automatically reconnect
     * @param callback The GATT callback
     * @return BluetoothGatt instance or null if failed
     */
    fun connectGatt(
        context: Context,
        device: BleDevice,
        autoConnect: Boolean,
        callback: BluetoothGattCallback
    ): BluetoothGatt?
    
    /**
     * Close the GATT connection
     * @param gatt The GATT instance to close
     */
    fun closeGatt(gatt: BluetoothGatt)
    
    /**
     * Disconnect the GATT connection
     * @param gatt The GATT instance to disconnect
     */
    fun disconnectGatt(gatt: BluetoothGatt)
}