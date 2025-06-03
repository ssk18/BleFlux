package com.ssk.bleflux.domain.scan

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.annotation.SuppressLint
import com.ssk.bleflux.domain.connection.BleDevice

data class BleScanResult(
    val device: BluetoothDevice,
    val rssi: Int,
    val scanRecord: ScanRecord?,
    val timestamp: Long = System.currentTimeMillis()
) {
    @SuppressLint("MissingPermission")
    val deviceName: String? = try {
        device.name
    } catch (e: SecurityException) {
        null
    }
    val deviceAddress: String = device.address
    
    /**
     * Convert scan result to a connectable BLE device
     */
    fun toBleDevice(): BleDevice {
        return BleDevice(
            bluetoothDevice = device,
            name = deviceName,
            address = deviceAddress,
            rssi = rssi
        )
    }
}