# BleFlux 🔵

A comprehensive BLE (Bluetooth Low Energy) library for Android with clean architecture, built with Kotlin and Jetpack Compose.

[![](https://jitpack.io/v/ssk18/BleFlux.svg)](https://jitpack.io/#ssk18/BleFlux)

## 📱 Features

- **Clean Architecture** - Separation of domain and data layers
- **Coroutines Support** - Fully async operations with Flow
- **Exception Handling** - Comprehensive error handling and recovery
- **Permission Management** - Built-in Bluetooth permission checks
- **Device Scanning** - Advanced BLE device discovery with filtering
- **Connection Management** - Robust device connection handling
- **GATT Operations** - Full support for BLE GATT operations
- **Hilt Integration** - Dependency injection ready

## 🚀 Installation

### Step 1: Add JitPack repository

Add JitPack to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add the dependency

Add BleFlux to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // Core domain module (required)
    implementation("com.github.ssk18.BleFlux:bleflux-domain:1.0.0")
    
    // Data implementation module (required)
    implementation("com.github.ssk18.BleFlux:bleflux-data:1.0.0")
}
```

### Step 3: Add permissions to AndroidManifest.xml

```xml
<!-- Required Bluetooth permissions -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

<!-- For Android 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Location permission (required for BLE scanning) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Optional: Course location for less precise location -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Declare that your app uses BLE -->
<uses-feature 
    android:name="android.hardware.bluetooth_le" 
    android:required="true" />
```

## 📖 Usage

### Initialize BleFlux

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BleFlux.initialize(this)
    }
}
```

### Scanning for Devices

```kotlin
@Composable
fun BleScanScreen() {
    val bleScanner = BleFlux.getScanner()
    val scanResults by bleScanner.scanResults.collectAsState()
    val scanState by bleScanner.scanState.collectAsState()
    
    LaunchedEffect(Unit) {
        try {
            bleScanner.startScan(
                scanTimeoutMs = 30000,
                scanFilters = null, // or provide ScanFilter list
                scanSettings = null // or provide custom ScanSettings
            )
        } catch (e: BleException) {
            // Handle scanning errors
            println("Scan failed: ${e.message}")
        }
    }
    
    LazyColumn {
        items(scanResults) { device ->
            DeviceItem(
                device = device,
                onClick = { /* Connect to device */ }
            )
        }
    }
}

@Composable
fun DeviceItem(device: BleScanResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = device.deviceName ?: "Unknown Device",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Address: ${device.deviceAddress}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "RSSI: ${device.rssi} dBm",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

### Connecting to a Device

```kotlin
class BleDeviceViewModel @Inject constructor(
    private val bleRepository: BleRepository
) : ViewModel() {
    
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()
    
    fun connectToDevice(device: BleScanResult) {
        viewModelScope.launch {
            try {
                val bleDevice = device.toBleDevice()
                bleRepository.connect(bleDevice)
                    .collect { state ->
                        _connectionState.value = state
                    }
            } catch (e: BleException) {
                // Handle connection errors
                _connectionState.value = BleConnectionState.Failed(e)
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            bleRepository.disconnect()
        }
    }
}
```

### Using with Hilt (Optional)

If you're using Hilt, the library provides ready-to-use modules:

```kotlin
@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BleFlux.initialize(this)
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var bleRepository: BleRepository
    
    // Use bleRepository...
}
```

## 🏗️ Architecture

```
┌─────────────────────┐
│   Presentation      │  ← Your app's UI layer
│   (Compose/Views)   │
└─────────────────────┘
           │
┌─────────────────────┐
│   Domain Layer      │  ← Business logic, use cases
│   (bleflux-domain)  │
└─────────────────────┘
           │
┌─────────────────────┐
│   Data Layer        │  ← Platform implementations
│   (bleflux-data)    │
└─────────────────────┘
```

## 🔧 Advanced Usage

### Custom Scan Filters

```kotlin
val scanFilters = listOf(
    ScanFilter.Builder()
        .setServiceUuid(ParcelUuid.fromString("your-service-uuid"))
        .build()
)

bleScanner.startScan(
    scanFilters = scanFilters,
    scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
)
```

### Error Handling

```kotlin
try {
    bleScanner.startScan()
} catch (e: BleScanException) {
    when (e) {
        is ScanStartException -> {
            // Handle scan start failures
            when (e.errorCode) {
                -1 -> println("Bluetooth adapter not available")
                -2 -> println("Missing permissions")
                -3 -> println("Permission denied")
            }
        }
        // Handle other BLE exceptions
    }
}
```

## 📄 License

```
Copyright 2024 Sameer Kulkarni

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/ssk18/BleFlux/issues) page
2. Create a new issue with detailed information
3. Include your Android version, device model, and error logs
