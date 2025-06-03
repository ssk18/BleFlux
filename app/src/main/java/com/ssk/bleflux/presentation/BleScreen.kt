package com.ssk.bleflux.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssk.bleflux.domain.connection.BleConnectionState
import com.ssk.bleflux.domain.scan.BleScanResult
import com.ssk.bleflux.domain.scan.BleScanState

@Composable
fun BleScreen(
    viewModel: BleViewModel,
    onRequestPermissions: () -> Unit,
    onRequestLocationSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scanState by viewModel.scanState.collectAsState(initial = BleScanState.Idle)
    val scanResults by viewModel.scanResults.collectAsState(initial = emptyList())
    val connectionState by viewModel.connectionState.collectAsState(initial = BleConnectionState.Disconnected)
    val uiState by viewModel.uiState.collectAsState(initial = BleUiState())
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BLE Scanner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ScanStateCard(scanState = scanState)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ConnectionStateCard(connectionState = connectionState, viewModel = viewModel)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ScanControlButtons(
            scanState = scanState,
            viewModel = viewModel,
            onRequestPermissions = onRequestPermissions,
            onRequestLocationSettings = onRequestLocationSettings
        )
        
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            ErrorCard(error = error)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        DevicesList(
            scanResults = scanResults,
            connectionState = connectionState,
            onConnectClick = { scanResult -> viewModel.connectToDevice(scanResult) }
        )
    }
}

@Composable
private fun ScanControlButtons(
    scanState: BleScanState,
    viewModel: BleViewModel,
    onRequestPermissions: () -> Unit,
    onRequestLocationSettings: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                val missingPermissions = viewModel.getMissingPermissions()
                if (missingPermissions.isNotEmpty()) {
                    onRequestPermissions()
                } else {
                    viewModel.startScan()
                }
            },
            enabled = scanState !is BleScanState.Scanning
        ) {
            Text("Start Scan")
        }
        
        Button(
            onClick = { viewModel.stopScan() },
            enabled = scanState is BleScanState.Scanning
        ) {
            Text("Stop")
        }
        
        Button(onClick = { viewModel.clearResults() }) {
            Text("Clear")
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun ConnectionStateCard(connectionState: BleConnectionState, viewModel: BleViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is BleConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is BleConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
                is BleConnectionState.Failed -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Connection Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = when (connectionState) {
                            BleConnectionState.Disconnected -> "Disconnected"
                            BleConnectionState.Connecting -> "Connecting..."
                            is BleConnectionState.Connected -> "Connected to ${connectionState.deviceAddress}"
                            is BleConnectionState.Failed -> "Failed: ${connectionState.exception.message}"
                            BleConnectionState.Disconnecting -> "Disconnecting..."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (connectionState is BleConnectionState.Connected) {
                    Button(
                        onClick = { viewModel.disconnect() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicesList(
    scanResults: List<BleScanResult>,
    connectionState: BleConnectionState,
    onConnectClick: (BleScanResult) -> Unit
) {
    if (scanResults.isNotEmpty()) {
        Text(
            text = "Found Devices (${scanResults.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(scanResults.sortedByDescending { it.rssi }) { result ->
                DeviceCard(
                    result = result,
                    connectionState = connectionState,
                    onConnectClick = onConnectClick
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        Text(
            text = "No devices found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScanStateCard(scanState: BleScanState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (scanState) {
                is BleScanState.Scanning -> MaterialTheme.colorScheme.primaryContainer
                is BleScanState.Failed -> MaterialTheme.colorScheme.errorContainer
                is BleScanState.TimedOut -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Scan Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = when (scanState) {
                    BleScanState.Idle -> "Ready to scan"
                    BleScanState.Starting -> "Starting scan..."
                    is BleScanState.Scanning -> "Scanning... (${(System.currentTimeMillis() - scanState.startTime) / 1000}s)"
                    is BleScanState.Failed -> "Failed: ${scanState.exception.message}"
                    BleScanState.Stopped -> "Scan stopped"
                    is BleScanState.TimedOut -> "Scan timed out after ${scanState.duration / 1000}s"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DeviceCard(
    result: BleScanResult,
    connectionState: BleConnectionState,
    onConnectClick: (BleScanResult) -> Unit
) {
    val isConnected = connectionState is BleConnectionState.Connected && 
                     connectionState.deviceAddress == result.deviceAddress
    val isConnecting = connectionState is BleConnectionState.Connecting
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer 
                           else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.deviceName ?: "Unknown Device",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "Address: ${result.deviceAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RSSI: ${result.rssi} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = getSignalStrengthText(result.rssi),
                            style = MaterialTheme.typography.bodySmall,
                            color = getSignalStrengthColor(result.rssi),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!isConnected && !isConnecting) {
                    Button(
                        onClick = { onConnectClick(result) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Connect")
                    }
                } else if (isConnected) {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else if (isConnecting) {
                    Text(
                        text = "Connecting...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun getSignalStrengthColor(rssi: Int): androidx.compose.ui.graphics.Color {
    return when {
        rssi >= -50 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green - Excellent
        rssi >= -60 -> androidx.compose.ui.graphics.Color(0xFF8BC34A) // Light Green - Good  
        rssi >= -70 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange - Fair
        rssi >= -80 -> androidx.compose.ui.graphics.Color(0xFFFF5722) // Red Orange - Poor
        else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red - Very Poor
    }
}

private fun getSignalStrengthText(rssi: Int): String {
    return when {
        rssi >= -50 -> "Excellent"
        rssi >= -60 -> "Good"
        rssi >= -70 -> "Fair" 
        rssi >= -80 -> "Poor"
        else -> "Very Poor"
    }
}