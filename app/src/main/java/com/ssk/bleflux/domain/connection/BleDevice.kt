package com.ssk.bleflux.domain.connection

import android.bluetooth.BluetoothDevice

/**
 * Represents a BLE device that can be connected to
 */
data class BleDevice(
    val bluetoothDevice: BluetoothDevice,
    val name: String?,
    val address: String,
    val rssi: Int? = null
) {
    val displayName: String
        get() = name ?: "Unknown Device"
}