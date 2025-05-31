package com.ssk.bleflux.domain.exceptions

import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanCallback

/**
 * Maps Android BLE status codes and error codes to custom BLE exceptions
 */
object BleExceptionMapper {

    /**
     * Maps GATT status codes to appropriate BLE exceptions
     */
    fun mapGattException(
        gattStatus: Int,
        operation: GattOperation,
        deviceAddress: String? = null,
        serviceUuid: String? = null,
        characteristicUuid: String? = null,
        descriptorUuid: String? = null
    ): BleException {
        return when (gattStatus) {
            BluetoothGatt.GATT_SUCCESS -> {
                // This shouldn't happen - success should not create an exception
                throw IllegalArgumentException("Cannot create exception for GATT_SUCCESS")
            }
            BluetoothGatt.GATT_FAILURE -> when (operation) {
                GattOperation.SERVICE_DISCOVERY -> ServiceDiscoveryException(gattStatus)
                GattOperation.CHARACTERISTIC_READ -> CharacteristicReadException(characteristicUuid!!, gattStatus)
                GattOperation.CHARACTERISTIC_WRITE -> CharacteristicWriteException(characteristicUuid!!, gattStatus)
                GattOperation.CHARACTERISTIC_NOTIFICATION -> CharacteristicNotificationException(characteristicUuid!!, gattStatus)
                GattOperation.DESCRIPTOR_READ -> DescriptorReadException(descriptorUuid!!, gattStatus)
                GattOperation.DESCRIPTOR_WRITE -> DescriptorWriteException(descriptorUuid!!, gattStatus)
                GattOperation.MTU_CHANGE -> MtuChangeException(0, gattStatus)
                GattOperation.PHY_CHANGE -> PhyChangeException(0, 0, gattStatus)
                GattOperation.RSSI_READ -> RssiReadException(gattStatus)
                GattOperation.CONNECTION -> ConnectionFailedException(deviceAddress!!, gattStatus)
            }
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> when (operation) {
                GattOperation.CHARACTERISTIC_READ -> CharacteristicReadException(characteristicUuid!!, gattStatus)
                GattOperation.DESCRIPTOR_READ -> DescriptorReadException(descriptorUuid!!, gattStatus)
                else -> GenericGattException(gattStatus, "Read not permitted for operation: $operation")
            }
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> when (operation) {
                GattOperation.CHARACTERISTIC_WRITE -> CharacteristicWriteException(characteristicUuid!!, gattStatus)
                GattOperation.DESCRIPTOR_WRITE -> DescriptorWriteException(descriptorUuid!!, gattStatus)
                else -> GenericGattException(gattStatus, "Write not permitted for operation: $operation")
            }
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> 
                GenericGattException(gattStatus, "Insufficient authentication for operation: $operation")
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> 
                GenericGattException(gattStatus, "Request not supported by remote device for operation: $operation")
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> 
                GenericGattException(gattStatus, "Insufficient encryption for operation: $operation")
            BluetoothGatt.GATT_INVALID_OFFSET -> 
                GenericGattException(gattStatus, "Invalid offset for operation: $operation")
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> 
                GenericGattException(gattStatus, "Invalid attribute length for operation: $operation")
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> 
                GenericGattException(gattStatus, "Connection congested during operation: $operation")
            133 -> DisconnectionException(deviceAddress ?: "unknown", gattStatus) // Common disconnection status
            else -> GenericGattException(gattStatus, "Unknown GATT error $gattStatus for operation: $operation")
        }
    }

    /**
     * Maps scan error codes to scan exceptions
     */
    fun mapScanException(errorCode: Int): BleScanException {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> 
                ScanStartException(errorCode, "Scan already started")
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> 
                ScanStartException(errorCode, "Application registration failed")
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> 
                ScanStartException(errorCode, "Feature unsupported")
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> 
                ScanStartException(errorCode, "Internal error")
            ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> 
                ScanStartException(errorCode, "Out of hardware resources")
            ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> 
                ScanStartException(errorCode, "Scanning too frequently")
            else -> ScanStartException(errorCode, "Unknown scan error")
        }
    }

    /**
     * Creates connection timeout exception
     */
    fun createConnectionTimeoutException(deviceAddress: String, timeoutMs: Long): ConnectionTimeoutException {
        return ConnectionTimeoutException(deviceAddress, timeoutMs)
    }

    /**
     * Creates operation timeout exception
     */
    fun createOperationTimeoutException(operation: String, timeoutMs: Long): OperationTimeoutException {
        return OperationTimeoutException(operation, timeoutMs)
    }

    /**
     * Creates service not found exception
     */
    fun createServiceNotFoundException(serviceUuid: String): ServiceNotFoundException {
        return ServiceNotFoundException(serviceUuid)
    }

    /**
     * Creates characteristic not found exception
     */
    fun createCharacteristicNotFoundException(characteristicUuid: String, serviceUuid: String): CharacteristicNotFoundException {
        return CharacteristicNotFoundException(characteristicUuid, serviceUuid)
    }

    /**
     * Creates descriptor not found exception
     */
    fun createDescriptorNotFoundException(descriptorUuid: String, characteristicUuid: String): DescriptorNotFoundException {
        return DescriptorNotFoundException(descriptorUuid, characteristicUuid)
    }

    /**
     * Creates device not connected exception
     */
    fun createDeviceNotConnectedException(deviceAddress: String): DeviceNotConnectedException {
        return DeviceNotConnectedException(deviceAddress)
    }

    /**
     * Creates concurrent operation exception
     */
    fun createConcurrentOperationException(operation: String): ConcurrentOperationException {
        return ConcurrentOperationException(operation)
    }

    /**
     * Creates operation cancelled exception
     */
    fun createOperationCancelledException(operation: String): OperationCancelledException {
        return OperationCancelledException(operation)
    }

    /**
     * Creates protocol-specific exceptions
     */
    fun createProtocolException(
        protocolName: String, 
        type: ProtocolExceptionType,
        message: String,
        command: String? = null,
        rawData: ByteArray? = null,
        expectedVersion: String? = null,
        actualVersion: String? = null
    ): BleProtocolException {
        return when (type) {
            ProtocolExceptionType.INITIALIZATION -> ProtocolInitializationException(protocolName, message)
            ProtocolExceptionType.COMMAND -> ProtocolCommandException(protocolName, command!!, message)
            ProtocolExceptionType.DATA_PARSING -> ProtocolDataParsingException(protocolName, rawData!!, message)
            ProtocolExceptionType.AUTHENTICATION -> ProtocolAuthenticationException(protocolName, message)
            ProtocolExceptionType.VERSION_MISMATCH -> ProtocolVersionMismatchException(protocolName, expectedVersion!!, actualVersion!!)
        }
    }
}

/**
 * Enum representing different GATT operations for exception mapping
 */
enum class GattOperation {
    CONNECTION,
    SERVICE_DISCOVERY,
    CHARACTERISTIC_READ,
    CHARACTERISTIC_WRITE,
    CHARACTERISTIC_NOTIFICATION,
    DESCRIPTOR_READ,
    DESCRIPTOR_WRITE,
    MTU_CHANGE,
    PHY_CHANGE,
    RSSI_READ
}

/**
 * Enum representing different protocol exception types
 */
enum class ProtocolExceptionType {
    INITIALIZATION,
    COMMAND,
    DATA_PARSING,
    AUTHENTICATION,
    VERSION_MISMATCH
}