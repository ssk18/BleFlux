package com.ssk.bleflux.data

import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
import androidx.core.content.ContextCompat
import com.ssk.bleflux.domain.BleFlux

private val context by lazy { BleFlux.context }

private fun getBleManagerOrNull(): BluetoothManager? =
    ContextCompat.getSystemService(context, BluetoothManager::class.java)

/** @throws IllegalStateException If bluetooth is unavailable. */
private fun getBluetoothManager(): BluetoothManager =
    getBleManagerOrNull() ?: error("BluetoothManager is not a supported system service")

internal fun getBleAdapterOrNull() =
    getBleManagerOrNull()?.adapter

/** @throws IllegalStateException If bluetooth is not supported. */
internal fun getBleAdapter() =
    getBluetoothManager().adapter ?: error("Bluetooth not supported")

internal suspend fun isSupported(): Boolean =
    context.packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE) &&
            getBleAdapterOrNull() != null