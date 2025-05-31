package com.ssk.bleflux.domain.scan

import android.Manifest
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ssk.bleflux.domain.exceptions.*

/**
 * Specialized exception handler for BLE scan operations
 */
class BleScanExceptionHandler(private val context: Context) {

    /**
     * Validates scan prerequisites and throws appropriate exceptions if not met
     */
    fun validateScanPrerequisites() {
        validateBleSupport()
        validatePermissions()
        validateLocationServices()
    }

    /**
     * Maps Android scan callback error codes to detailed exceptions
     */
    fun mapScanCallbackError(errorCode: Int): BleScanException {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> {
                ScanStartException(
                    errorCode = errorCode,
                    message = "Scan is already in progress. Stop the current scan before starting a new one."
                )
            }
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                ScanStartException(
                    errorCode = errorCode,
                    message = "Failed to register application for scanning. This usually indicates a system-level issue or too many scan applications."
                )
            }
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> {
                ScanStartException(
                    errorCode = errorCode,
                    message = "The scanning feature is not supported on this device or Android version."
                )
            }
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> {
                ScanStartException(
                    errorCode = errorCode,
                    message = "Internal Bluetooth stack error occurred during scan initialization."
                )
            }
            ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> {
                ScanStartException(
                    errorCode = errorCode,
                    message = "Insufficient hardware resources available for scanning. Try stopping other BLE operations."
                )
            }
            ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> {
                ScanStartException(
                    errorCode = errorCode,
                    message = "Scanning requests are being made too frequently. Wait before starting another scan."
                )
            }
            else -> {
                ScanStartException(
                    errorCode = errorCode,
                    message = "Unknown scan error occurred with code: $errorCode"
                )
            }
        }
    }

    /**
     * Creates timeout exception for scan operations
     */
    fun createScanTimeoutException(timeoutMs: Long): ScanTimeoutException {
        return ScanTimeoutException(timeoutMs)
    }

    /**
     * Creates scan filter validation exception
     */
    fun createScanFilterException(reason: String): ScanFilterException {
        return ScanFilterException(reason)
    }

    /**
     * Validates that the device supports BLE
     */
    private fun validateBleSupport() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw BleNotSupportedException("BLE is not supported on this device")
        }
    }

    /**
     * Validates required permissions for scanning
     */
    private fun validatePermissions() {
        val missingPermissions = mutableListOf<String>()

        // Check location permissions (required for BLE scanning)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Check Android 12+ specific permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            throw BlePermissionDeniedException(missingPermissions.joinToString(", "))
        }
    }

    /**
     * Validates that location services are enabled (required for BLE scanning on Android 6+)
     */
    private fun validateLocationServices() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            throw BleLocationDisabledException(
                "Location services must be enabled for BLE scanning on Android 6.0 and above"
            )
        }
    }

    /**
     * Provides detailed error recovery suggestions
     */
    fun getErrorRecoverySuggestion(exception: BleScanException): String {
        return when (exception) {
            is ScanStartException -> when (exception.errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> 
                    "Stop the current scan using stopScan() before starting a new one."
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> 
                    "Restart the application or wait before retrying. Consider reducing concurrent BLE operations."
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> 
                    "This device does not support the requested scan features. Use basic scan parameters."
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> 
                    "Restart Bluetooth or the application. If the issue persists, restart the device."
                ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> 
                    "Stop other BLE operations (connections, advertising) and retry scanning."
                ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> 
                    "Wait at least 30 seconds before starting another scan operation."
                else -> "Check device capabilities and retry the scan operation."
            }
            is ScanTimeoutException -> 
                "Increase scan timeout duration or check if target devices are in range and advertising."
            is ScanFilterException -> 
                "Review scan filter parameters. Ensure UUIDs are valid and filter criteria are not too restrictive."
            else -> "Check BLE permissions, location services, and device capabilities."
        }
    }
}

/**
 * Detailed scan state for tracking scan progress and errors
 */
sealed class BleScanState {
    data object Idle : BleScanState()
    data object Starting : BleScanState()
    data class Scanning(val startTime: Long = System.currentTimeMillis()) : BleScanState()
    data class Failed(val exception: BleScanException, val canRetry: Boolean = true) : BleScanState()
    data object Stopped : BleScanState()
    data class TimedOut(val duration: Long) : BleScanState()
}

/**
 * Scan configuration validation
 */
object ScanConfigValidator {
    
    fun validateScanSettings(
        scanTimeoutMs: Long?,
        batchScanMode: Boolean = false,
        highPowerMode: Boolean = false
    ) {
        // Validate timeout
        scanTimeoutMs?.let { timeout ->
            if (timeout <= 0) {
                throw ScanFilterException("Scan timeout must be positive, got: $timeout ms")
            }
            if (timeout > 300_000) { // 5 minutes max
                throw ScanFilterException("Scan timeout too long, maximum 300,000 ms, got: $timeout ms")
            }
        }

        // Validate batch scan mode compatibility
        if (batchScanMode && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw ScanFilterException("Batch scan mode requires Android API 21 or higher")
        }

        // Validate high power mode implications
        if (highPowerMode) {
            // Warning: High power mode can drain battery quickly
        }
    }

    fun validateScanFilters(serviceUuids: List<String>?, deviceName: String?, deviceAddress: String?) {
        // Validate service UUIDs format
        serviceUuids?.forEach { uuid ->
            if (!isValidUuid(uuid)) {
                throw ScanFilterException("Invalid service UUID format: $uuid")
            }
        }

        // Validate device name
        deviceName?.let { name ->
            if (name.isEmpty()) {
                throw ScanFilterException("Device name cannot be empty")
            }
            if (name.length > 248) { // BLE advertising name limit
                throw ScanFilterException("Device name too long, maximum 248 characters")
            }
        }

        // Validate device address format
        deviceAddress?.let { address ->
            if (!isValidMacAddress(address)) {
                throw ScanFilterException("Invalid device address format: $address")
            }
        }
    }

    private fun isValidUuid(uuid: String): Boolean {
        return try {
            java.util.UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun isValidMacAddress(address: String): Boolean {
        val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
        return macPattern.matches(address)
    }
}