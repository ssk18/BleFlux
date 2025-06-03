package com.ssk.bleflux.data.repository

import com.ssk.bleflux.domain.connection.BleConnection
import com.ssk.bleflux.domain.connection.BleConnectionState
import com.ssk.bleflux.domain.connection.BleDevice
import com.ssk.bleflux.domain.repository.BleRepository
import com.ssk.bleflux.domain.scan.BleScanResult
import com.ssk.bleflux.domain.scan.BleScanState
import com.ssk.bleflux.domain.scan.BleScanner
import com.ssk.bleflux.domain.scan.BlePermissionChecker
import kotlinx.coroutines.flow.Flow

class BleRepositoryImpl(
    private val bleScanner: BleScanner,
    private val bleConnection: BleConnection,
    private val permissionChecker: BlePermissionChecker
) : BleRepository {
    
    // Scanning
    override val scanState: Flow<BleScanState> = bleScanner.scanState
    override val scanResults: Flow<List<BleScanResult>> = bleScanner.scanResults
    
    override suspend fun startScan(timeoutMs: Long): Result<Unit> {
        return try {
            bleScanner.startScan(timeoutMs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopScan(): Result<Unit> {
        return try {
            bleScanner.stopScan()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun clearResults() {
        bleScanner.clearResults()
    }
    
    // Connection
    override val connectionState: Flow<BleConnectionState> = bleConnection.connectionState
    
    override suspend fun connect(device: BleDevice, autoConnect: Boolean, timeoutMs: Long): Result<Unit> {
        return bleConnection.connect(device, autoConnect, timeoutMs)
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return bleConnection.disconnect()
    }
    
    override fun isConnected(): Boolean {
        return bleConnection.isConnected()
    }
    
    override fun getConnectedDevice(): BleDevice? {
        return bleConnection.getConnectedDevice()
    }
    
    override suspend fun readRssi(): Result<Int> {
        return bleConnection.readRssi()
    }
    
    // Permissions
    override fun hasBluetoothPermissions(): Boolean {
        return permissionChecker.hasBluetoothPermissions()
    }
    
    override fun getMissingPermissions(): List<String> {
        return permissionChecker.getMissingPermissions()
    }
}