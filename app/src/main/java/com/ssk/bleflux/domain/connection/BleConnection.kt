package com.ssk.bleflux.domain.connection

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing BLE connections
 */
interface BleConnection {
    
    /**
     * Observable connection state
     */
    val connectionState: StateFlow<BleConnectionState>
    
    /**
     * Connect to a BLE device
     * @param device The device to connect to
     * @param autoConnect Whether to automatically reconnect if connection is lost
     * @param timeoutMs Connection timeout in milliseconds
     * @return Result indicating success or failure
     */
    suspend fun connect(
        device: BleDevice,
        autoConnect: Boolean = false,
        timeoutMs: Long = 10000
    ): Result<Unit>
    
    /**
     * Disconnect from the currently connected device
     * @return Result indicating success or failure
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Check if currently connected to a device
     */
    fun isConnected(): Boolean
    
    /**
     * Get the currently connected device, if any
     */
    fun getConnectedDevice(): BleDevice?
    
    /**
     * Read RSSI of the connected device
     */
    suspend fun readRssi(): Result<Int>
}