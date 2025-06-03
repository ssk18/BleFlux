package com.ssk.bleflux.data.scan

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
import android.content.Context
import androidx.core.content.ContextCompat
import com.ssk.bleflux.domain.scan.BleAdapterProvider

class BleAdapterProviderImpl(private val context: Context) : BleAdapterProvider {
    override suspend fun isSupported(): Boolean =  context.packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE) &&
            getBleAdapterOrNull() != null

    internal fun getBleManagerOrNull(): BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)
    
    override fun getBleAdapter(): BluetoothAdapter =  getBleManagerOrNull()?.adapter ?: error("Bluetooth not supported")
    
    override fun getBleAdapterOrNull(): BluetoothAdapter? = getBleManagerOrNull()?.adapter
    
    override fun getBluetoothLeScanner(): BluetoothLeScanner? = 
       getBleAdapterOrNull()?.bluetoothLeScanner
    
    @Throws(SecurityException::class)
    override fun startScan(
        scanner: BluetoothLeScanner,
        filters: List<ScanFilter>?,
        settings: ScanSettings,
        callback: ScanCallback
    ) {
        scanner.startScan(filters, settings, callback)
    }
    
    @Throws(SecurityException::class)
    override fun stopScan(scanner: BluetoothLeScanner, callback: ScanCallback) {
        scanner.stopScan(callback)
    }
}