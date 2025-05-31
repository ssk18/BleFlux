package com.ssk.bleflux.domain.exceptions

import android.bluetooth.BluetoothGatt

/**
 * Base sealed class for all BLE-related exceptions
 */
sealed class BleException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

// Core BLE Exceptions
class BleNotSupportedException(message: String = "BLE is not supported on this device") : BleException(message)
class BleNotEnabledException(message: String = "Bluetooth is not enabled") : BleException(message)
class BlePermissionDeniedException(val permission: String) : BleException("BLE permission denied: $permission")
class BleLocationDisabledException(message: String = "Location services are disabled") : BleException(message)

// Connection Exceptions
sealed class BleConnectionException(message: String, cause: Throwable? = null) : BleException(message, cause)
class DeviceNotConnectedException(val deviceAddress: String) : BleConnectionException("Device $deviceAddress is not connected")
class ConnectionTimeoutException(val deviceAddress: String, val timeoutMs: Long) : BleConnectionException("Connection to $deviceAddress timed out after ${timeoutMs}ms")
class ConnectionFailedException(val deviceAddress: String, val gattStatus: Int) : BleConnectionException("Failed to connect to $deviceAddress, GATT status: $gattStatus")
class DisconnectionException(val deviceAddress: String, val gattStatus: Int) : BleConnectionException("Unexpected disconnection from $deviceAddress, GATT status: $gattStatus")

// GATT Operation Exceptions
sealed class BleGattException(
    val gattStatus: Int,
    message: String,
    cause: Throwable? = null
) : BleException(message, cause)

class GenericGattException(gattStatus: Int, message: String) : BleGattException(gattStatus, message)

class ServiceDiscoveryException(gattStatus: Int) : BleGattException(gattStatus, "Service discovery failed with status: $gattStatus")
class ServiceNotFoundException(val serviceUuid: String) : BleGattException(BluetoothGatt.GATT_FAILURE, "Service not found: $serviceUuid")
class CharacteristicNotFoundException(val characteristicUuid: String, val serviceUuid: String) : BleGattException(BluetoothGatt.GATT_FAILURE, "Characteristic $characteristicUuid not found in service $serviceUuid")
class DescriptorNotFoundException(val descriptorUuid: String, val characteristicUuid: String) : BleGattException(BluetoothGatt.GATT_FAILURE, "Descriptor $descriptorUuid not found in characteristic $characteristicUuid")

// Read/Write/Notify Exceptions
class CharacteristicReadException(val characteristicUuid: String, gattStatus: Int) : BleGattException(gattStatus, "Failed to read characteristic $characteristicUuid, status: $gattStatus")
class CharacteristicWriteException(val characteristicUuid: String, gattStatus: Int) : BleGattException(gattStatus, "Failed to write to characteristic $characteristicUuid, status: $gattStatus")
class CharacteristicNotificationException(val characteristicUuid: String, gattStatus: Int) : BleGattException(gattStatus, "Failed to enable/disable notification for characteristic $characteristicUuid, status: $gattStatus")
class DescriptorWriteException(val descriptorUuid: String, gattStatus: Int) : BleGattException(gattStatus, "Failed to write descriptor $descriptorUuid, status: $gattStatus")
class DescriptorReadException(val descriptorUuid: String, gattStatus: Int) : BleGattException(gattStatus, "Failed to read descriptor $descriptorUuid, status: $gattStatus")

// MTU and PHY Exceptions
class MtuChangeException(val requestedMtu: Int, gattStatus: Int) : BleGattException(gattStatus, "Failed to change MTU to $requestedMtu, status: $gattStatus")
class PhyChangeException(val txPhy: Int, val rxPhy: Int, gattStatus: Int) : BleGattException(gattStatus, "Failed to change PHY to tx:$txPhy rx:$rxPhy, status: $gattStatus")
class RssiReadException(gattStatus: Int) : BleGattException(gattStatus, "Failed to read RSSI, status: $gattStatus")

// Scan Exceptions
sealed class BleScanException(message: String, cause: Throwable? = null) : BleException(message, cause)
class ScanStartException(val errorCode: Int, message: String) : BleScanException("Scan failed to start with error code $errorCode: $message")
class ScanTimeoutException(val timeoutMs: Long) : BleScanException("Scan timed out after ${timeoutMs}ms")
class ScanFilterException(message: String) : BleScanException("Invalid scan filter: $message")

// Protocol-specific Exceptions
sealed class BleProtocolException(
    val protocolName: String,
    message: String,
    cause: Throwable? = null
) : BleException("[$protocolName] $message", cause)

class ProtocolInitializationException(protocolName: String, message: String) : BleProtocolException(protocolName, "Initialization failed: $message")
class ProtocolCommandException(protocolName: String, val command: String, message: String) : BleProtocolException(protocolName, "Command '$command' failed: $message")
class ProtocolDataParsingException(protocolName: String, val rawData: ByteArray, message: String) : BleProtocolException(protocolName, "Data parsing failed: $message")
class ProtocolAuthenticationException(protocolName: String, message: String) : BleProtocolException(protocolName, "Authentication failed: $message")
class ProtocolVersionMismatchException(protocolName: String, val expectedVersion: String, val actualVersion: String) : BleProtocolException(protocolName, "Version mismatch - expected: $expectedVersion, actual: $actualVersion")

// Operation Exceptions
class OperationCancelledException(val operation: String) : BleException("Operation cancelled: $operation")
class OperationTimeoutException(val operation: String, val timeoutMs: Long) : BleException("Operation '$operation' timed out after ${timeoutMs}ms")
class ConcurrentOperationException(val operation: String) : BleException("Cannot perform '$operation' while another operation is in progress")
class UnsupportedOperationException(val operation: String, val reason: String) : BleException("Operation '$operation' is not supported: $reason")

// Data Validation Exceptions
class InvalidDataException(val expectedFormat: String, val actualData: ByteArray) : BleException("Invalid data format - expected: $expectedFormat, got: ${actualData.contentToString()}")
class DataSizeException(val expectedSize: Int, val actualSize: Int) : BleException("Invalid data size - expected: $expectedSize bytes, got: $actualSize bytes")
class ChecksumException(val expected: String, val actual: String) : BleException("Checksum mismatch - expected: $expected, actual: $actual")

// Generic BLE Exception for unknown errors
class GenericBleException(message: String, cause: Throwable? = null) : BleException(message, cause)