package com.ssk.bleflux.domain.scan

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import com.ssk.bleflux.domain.connection.BleDevice

data class BleScanResult(
    val device: BluetoothDevice,
    val rssi: Int,
    val scanRecord: ScanRecord?,
    val timestamp: Long = System.currentTimeMillis()
) {
    val deviceName: String? = device.name
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