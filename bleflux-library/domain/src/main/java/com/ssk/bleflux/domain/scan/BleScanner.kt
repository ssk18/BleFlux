package com.ssk.bleflux.domain.scan

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import com.ssk.bleflux.domain.concurrency.BleCoroutineScope
import com.ssk.bleflux.domain.exceptions.ScanStartException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap

class BleScanner(
    private val bleAdapterProvider: BleAdapterProvider,
    private val permissionChecker: BlePermissionChecker,
    private val scanExceptionHandler: BleScanExceptionHandler
) {
    private val bleScope = BleCoroutineScope()
    
    private val _scanState = MutableStateFlow<BleScanState>(BleScanState.Idle)
    val scanState: StateFlow<BleScanState> = _scanState.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<BleScanResult>>(emptyList())
    val scanResults: StateFlow<List<BleScanResult>> = _scanResults.asStateFlow()
    
    private val discoveredDevices = ConcurrentHashMap<String, BleScanResult>()
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanResult = BleScanResult(
                device = result.device,
                rssi = result.rssi,
                scanRecord = result.scanRecord
            )
            
            discoveredDevices[result.device.address] = scanResult
            _scanResults.update {
                discoveredDevices.values.toList()
            }
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                val scanResult = BleScanResult(
                    device = result.device,
                    rssi = result.rssi,
                    scanRecord = result.scanRecord
                )
                discoveredDevices[result.device.address] = scanResult
            }
            _scanResults.update {
                discoveredDevices.values.toList()
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            val exception = scanExceptionHandler.mapScanCallbackError(errorCode)
            _scanState.value = BleScanState.Failed(
                exception = exception,
                canRetry = errorCode != ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED
            )
        }
    }
    
    suspend fun startScan(
        scanTimeoutMs: Long = 30000,
        scanFilters: List<ScanFilter>? = null,
        scanSettings: ScanSettings? = null
    ) {
        bleScope.executeOrThrow("start_scan", scanTimeoutMs + 5000) {
            // Validate prerequisites first
            scanExceptionHandler.validateScanPrerequisites()
            
            bluetoothLeScanner = bleAdapterProvider.getBluetoothLeScanner()
            
            if (bluetoothLeScanner == null) {
                val adapter = bleAdapterProvider.getBleAdapterOrNull()
                throw ScanStartException(
                    errorCode = -1,
                    message = "BluetoothLeScanner is null. Adapter: $adapter, isEnabled: ${adapter?.isEnabled}"
                )
            }
            
            if (!permissionChecker.hasBluetoothPermissions()) {
                val missingPermissions = permissionChecker.getMissingPermissions()
                throw ScanStartException(
                    errorCode = -2,
                    message = "Missing permissions: ${missingPermissions.joinToString()}"
                )
            }
            
            _scanState.value = BleScanState.Starting
            discoveredDevices.clear()
            _scanResults.value = emptyList()
            
            val settings = scanSettings ?: ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()
            
            bluetoothLeScanner?.let { scanner ->
                try {
                    bleAdapterProvider.startScan(scanner, scanFilters, settings, scanCallback)
                } catch (e: SecurityException) {
                    throw ScanStartException(
                        errorCode = -3,
                        message = "Permission denied: ${e.message}"
                    )
                }
            }
            _scanState.value = BleScanState.Scanning(System.currentTimeMillis())
            
            try {
                // Use suspendCancellableCoroutine for proper cancellation support
                suspendCancellableCoroutine<Unit> { continuation ->
                    val timeoutJob = launch {
                        delay(scanTimeoutMs)
                        if (_scanState.value is BleScanState.Scanning) {
                            stopScan()
                            _scanState.value = BleScanState.TimedOut(scanTimeoutMs)
                        }
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(Unit))
                        }
                    }
                    
                    continuation.invokeOnCancellation { 
                        timeoutJob.cancel()
                        stopScan()
                    }
                }
            } catch (e: CancellationException) {
                stopScan()
                throw e // Always rethrow CancellationException
            }
        }
    }
    
    fun stopScan() {
        try {
            bluetoothLeScanner?.let { scanner ->
                if (permissionChecker.hasBluetoothPermissions()) {
                    try {
                        bleAdapterProvider.stopScan(scanner, scanCallback)
                    } catch (e: SecurityException) {
                        _scanState.value = BleScanState.Failed(
                            exception = ScanStartException(-3, "Permission denied: ${e.message}"),
                            canRetry = false
                        )
                        return
                    }
                }
            }
            _scanState.value = BleScanState.Stopped
        } catch (e: Exception) {
            _scanState.value = BleScanState.Failed(
                exception = ScanStartException(-1, "Failed to stop scan: ${e.message}"),
                canRetry = false
            )
        }
    }
    
    fun clearResults() {
        discoveredDevices.clear()
        _scanResults.value = emptyList()
    }
    
}