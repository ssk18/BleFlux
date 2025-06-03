package com.ssk.bleflux.domain.repository

import com.ssk.bleflux.domain.connection.BleConnectionState
import com.ssk.bleflux.domain.connection.BleDevice
import com.ssk.bleflux.domain.scan.BleScanResult
import com.ssk.bleflux.domain.scan.BleScanState
import kotlinx.coroutines.flow.Flow

interface BleRepository {
    // Scanning
    val scanState: Flow<BleScanState>
    val scanResults: Flow<List<BleScanResult>>
    
    suspend fun startScan(timeoutMs: Long = 30000): Result<Unit>
    suspend fun stopScan(): Result<Unit>
    fun clearResults()
    
    // Connection
    val connectionState: Flow<BleConnectionState>
    
    suspend fun connect(device: BleDevice, autoConnect: Boolean = false, timeoutMs: Long = 10000): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    fun isConnected(): Boolean
    fun getConnectedDevice(): BleDevice?
    suspend fun readRssi(): Result<Int>
    
    // Permissions
    fun hasBluetoothPermissions(): Boolean
    fun getMissingPermissions(): List<String>
}