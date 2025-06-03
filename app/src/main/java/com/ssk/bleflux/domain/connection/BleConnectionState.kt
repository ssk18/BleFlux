package com.ssk.bleflux.domain.connection

import com.ssk.bleflux.domain.exceptions.BleConnectionException

/**
 * Represents the state of a BLE connection
 */
sealed class BleConnectionState {
    data object Disconnected : BleConnectionState()
    data object Connecting : BleConnectionState()
    data class Connected(val deviceAddress: String) : BleConnectionState()
    data class Failed(val exception: BleConnectionException, val canRetry: Boolean = true) : BleConnectionState()
    data object Disconnecting : BleConnectionState()
}