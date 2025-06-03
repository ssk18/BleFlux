package com.ssk.bleflux.domain.scan

interface BlePermissionChecker {
    fun hasBluetoothPermissions(): Boolean
    fun getMissingPermissions(): List<String>
}