package com.ssk.bleflux.data.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.ssk.bleflux.domain.connection.BleConnection
import com.ssk.bleflux.domain.connection.BleConnectionState
import com.ssk.bleflux.domain.connection.BleDevice
import com.ssk.bleflux.domain.connection.BleGattProvider
import com.ssk.bleflux.domain.exceptions.ConnectionFailedException
import com.ssk.bleflux.domain.exceptions.ConnectionTimeoutException
import com.ssk.bleflux.domain.exceptions.DeviceNotConnectedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BleConnectionImpl(
    private val context: Context,
    private val gattProvider: BleGattProvider
) : BleConnection {
    
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BleDevice? = null
    private var pendingContinuation: Continuation<Unit>? = null
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.Connected(connectedDevice?.address ?: "")
                    pendingContinuation?.resume(Unit)
                    pendingContinuation = null
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Disconnected
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        val exception = ConnectionFailedException(connectedDevice?.address ?: "", status)
                        _connectionState.value = BleConnectionState.Failed(exception, canRetry = true)
                        pendingContinuation?.resumeWithException(exception)
                    }
                    pendingContinuation = null
                    cleanup()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = BleConnectionState.Connecting
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _connectionState.value = BleConnectionState.Disconnecting
                }
            }
        }
        
        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            // Handle RSSI reading result
        }
    }
    
    override suspend fun connect(
        device: BleDevice,
        autoConnect: Boolean,
        timeoutMs: Long
    ): Result<Unit> {
        return try {
            if (isConnected()) {
                disconnect()
            }
            
            _connectionState.value = BleConnectionState.Connecting
            connectedDevice = device
            
            bluetoothGatt = gattProvider.connectGatt(context, device, autoConnect, gattCallback)
            
            if (bluetoothGatt == null) {
                val exception = ConnectionFailedException(device.address, -1)
                _connectionState.value = BleConnectionState.Failed(exception)
                throw exception
            }
            
            // Wait for connection with timeout
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    // Store continuation to use in callback
                    pendingContinuation = continuation
                    
                    continuation.invokeOnCancellation {
                        pendingContinuation = null
                        cleanup()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            cleanup()
            when (e) {
                is kotlinx.coroutines.TimeoutCancellationException -> {
                    val timeoutException = ConnectionTimeoutException(device.address, timeoutMs)
                    _connectionState.value = BleConnectionState.Failed(timeoutException)
                    Result.failure(timeoutException)
                }
                else -> Result.failure(e)
            }
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return try {
            val gatt = bluetoothGatt
            if (gatt == null || !isConnected()) {
                _connectionState.update {
                    BleConnectionState.Disconnected
                }
                cleanup()
                return Result.success(Unit)
            }
            _connectionState.update {
                BleConnectionState.Disconnecting
            }
            suspendCancellableCoroutine { continuation ->
                pendingContinuation = continuation
                gattProvider.disconnectGatt(gatt)
                continuation.invokeOnCancellation {
                    pendingContinuation = null
                    cleanup()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            cleanup()
            Result.failure(e)
        }
    }
    
    override fun isConnected(): Boolean {
        return connectionState.value is BleConnectionState.Connected
    }
    
    override fun getConnectedDevice(): BleDevice? {
        return if (isConnected()) connectedDevice else null
    }
    
    override suspend fun readRssi(): Result<Int> {
        return try {
            val gatt = bluetoothGatt
            if (gatt == null || !isConnected()) {
                return Result.failure(DeviceNotConnectedException(connectedDevice?.address ?: ""))
            }
            
            suspendCancellableCoroutine { continuation ->
                val success = gatt.readRemoteRssi()
                if (!success) {
                    continuation.resumeWithException(Exception("Failed to read RSSI"))
                }
                // Result will be delivered in onReadRemoteRssi callback
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun cleanup() {
        bluetoothGatt?.let { gatt ->
            gattProvider.closeGatt(gatt)
        }
        bluetoothGatt = null
        connectedDevice = null
    }
}