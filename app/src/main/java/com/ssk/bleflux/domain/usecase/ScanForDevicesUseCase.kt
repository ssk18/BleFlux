package com.ssk.bleflux.domain.usecase

import com.ssk.bleflux.domain.repository.BleRepository

class ScanForDevicesUseCase(
    private val bleRepository: BleRepository
) {
    suspend operator fun invoke(timeoutMs: Long = 30000): Result<Unit> {
        return if (bleRepository.hasBluetoothPermissions()) {
            bleRepository.startScan(timeoutMs)
        } else {
            Result.failure(
                SecurityException("Missing permissions: ${bleRepository.getMissingPermissions().joinToString()}")
            )
        }
    }
}