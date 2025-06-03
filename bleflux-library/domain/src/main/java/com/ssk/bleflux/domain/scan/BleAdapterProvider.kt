package com.ssk.bleflux.domain.scan

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings

interface BleAdapterProvider {
    suspend fun isSupported(): Boolean
    fun getBleAdapter(): BluetoothAdapter
    fun getBleAdapterOrNull(): BluetoothAdapter?
    fun getBluetoothLeScanner(): BluetoothLeScanner?
    
    @Throws(SecurityException::class)
    fun startScan(
        scanner: BluetoothLeScanner,
        filters: List<ScanFilter>?,
        settings: ScanSettings,
        callback: ScanCallback
    )
    
    @Throws(SecurityException::class)
    fun stopScan(scanner: BluetoothLeScanner, callback: ScanCallback)
}