package com.ssk.bleflux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssk.bleflux.domain.connection.BleConnectionState
import com.ssk.bleflux.domain.repository.BleRepository
import com.ssk.bleflux.domain.scan.BleScanResult
import com.ssk.bleflux.domain.scan.BleScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BleViewModel @Inject constructor(
    private val bleRepository: BleRepository
) : ViewModel() {
    
    val scanState = bleRepository.scanState
    val scanResults = bleRepository.scanResults
    val connectionState = bleRepository.connectionState

    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState.asStateFlow()
    
    fun startScan() {
        if (!bleRepository.hasBluetoothPermissions()) {
            _uiState.value = _uiState.value.copy(
                error = "Missing permissions: ${bleRepository.getMissingPermissions().joinToString()}"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                bleRepository.startScan().fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(error = null)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Don't update UI state on cancellation - let it be handled by structured concurrency
                throw e
            }
        }
    }
    
    fun stopScan() {
        viewModelScope.launch {
            try {
                bleRepository.stopScan().fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(error = null)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Don't update UI state on cancellation - let it be handled by structured concurrency
                throw e
            }
        }
    }
    
    fun clearResults() {
        bleRepository.clearResults()
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun connectToDevice(scanResult: BleScanResult) {
        if (!bleRepository.hasBluetoothPermissions()) {
            _uiState.value = _uiState.value.copy(
                error = "Missing permissions: ${bleRepository.getMissingPermissions().joinToString()}"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val device = scanResult.toBleDevice()
                bleRepository.connect(device).fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(error = null)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Don't update UI state on cancellation - let it be handled by structured concurrency
                throw e
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            try {
                bleRepository.disconnect().fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(error = null)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Don't update UI state on cancellation - let it be handled by structured concurrency
                throw e
            }
        }
    }
    
    fun getMissingPermissions(): List<String> {
        return bleRepository.getMissingPermissions()
    }
}

data class BleUiState(
    val error: String? = null
)