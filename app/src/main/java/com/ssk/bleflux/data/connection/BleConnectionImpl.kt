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
import kotlinx.coroutines.CancellationException
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
                    _connectionState.update { 
                        BleConnectionState.Connected(connectedDevice?.address ?: "") 
                    }
                    pendingContinuation?.resume(Unit)
                    pendingContinuation = null
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        val exception = ConnectionFailedException(connectedDevice?.address ?: "", status)
                        _connectionState.update { 
                            BleConnectionState.Failed(exception, canRetry = true) 
                        }
                        pendingContinuation?.resumeWithException(exception)
                    } else {
                        _connectionState.update { BleConnectionState.Disconnected }
                        // Successful disconnection
                        pendingContinuation?.resume(Unit)
                    }
                    pendingContinuation = null
                    cleanup()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.update { BleConnectionState.Connecting }
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _connectionState.update { BleConnectionState.Disconnecting }
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
            // Ensure we're disconnected first
            if (isConnected()) {
                disconnect().getOrThrow()
            }
            
            _connectionState.update { BleConnectionState.Connecting }
            connectedDevice = device
            
            bluetoothGatt = gattProvider.connectGatt(context, device, autoConnect, gattCallback)
            
            if (bluetoothGatt == null) {
                val exception = ConnectionFailedException(device.address, -1)
                _connectionState.update { BleConnectionState.Failed(exception) }
                return Result.failure(exception)
            }
            
            // Wait for connection with proper timeout and cancellation handling
            try {
                withTimeout(timeoutMs) {
                    suspendCancellableCoroutine { continuation ->
                        pendingContinuation = continuation
                        
                        continuation.invokeOnCancellation {
                            pendingContinuation = null
                            cleanup()
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                val timeoutException = ConnectionTimeoutException(device.address, timeoutMs)
                _connectionState.update { BleConnectionState.Failed(timeoutException) }
                cleanup()
                Result.failure(timeoutException)
            } catch (e: CancellationException) {
                // Always cleanup on cancellation and rethrow
                cleanup()
                throw e
            }
        } catch (e: CancellationException) {
            // Never catch and wrap CancellationException
            throw e
        } catch (e: Exception) {
            cleanup()
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return try {
            val gatt = bluetoothGatt
            if (gatt == null || !isConnected()) {
                _connectionState.value = BleConnectionState.Disconnected
                cleanup()
                return Result.success(Unit)
            }
            
            _connectionState.value = BleConnectionState.Disconnecting
            
            // Wait for disconnection callback
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