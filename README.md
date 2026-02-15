# Dezibot Android SDK

[![](https://jitpack.io/v/lfranke42/dezibot-android-sdk.svg)](https://jitpack.io/#lfranke42/dezibot-android-sdk)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An Android SDK for controlling ESP32-based **Dezibot** robots. The SDK creates a WiFi hotspot, advertises credentials via Bluetooth Low Energy (BLE), and communicates with connected Dezibot devices over WebSocket.

## Features

- **WiFi Hotspot**: Automatically creates a local-only WiFi hotspot for Dezibot devices to connect
- **BLE Credential Sharing**: Advertises hotspot credentials via BLE GATT so Dezibot devices can discover and connect automatically
- **WebSocket Communication**: Real-time bidirectional communication with connected Dezibot devices
- **Multi-Device Support**: Control multiple Dezibot devices simultaneously with selective broadcasting
- **Reactive State**: Kotlin StateFlows for observing connected devices, hosting state, and sensor readings

## Demo App

A demo application showcasing the SDK is available at:  
👉 [Dezibot Demo App](https://gitlab.dit.htwk-leipzig.de/dezibot-android-sdk/dezibot-demo-app)

## ⚠️ Firmware Requirement

**Important**: Dezibot devices must be running the special client firmware to connect to the Android host.

The required firmware is available here:  
👉 [Dezibot Client Firmware](https://gitlab.dit.htwk-leipzig.de/dezibot-android-sdk/websocket)

Without this firmware, Dezibot devices will not be able to:
- Discover the BLE GATT service
- Connect to the WiFi hotspot
- Communicate via WebSocket

Please flash your Dezibot devices with this firmware before using the SDK.

## Requirements

- **Android 8.0+** (API level 26)
- **Kotlin** 1.9+
- Device with **WiFi** and **Bluetooth LE** support
- **Dezibot devices with WebSocket firmware** (see above)

## Installation

### Step 1: Add JitPack Repository

Add the JitPack repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add Dependency

Add the SDK dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.lfranke42:dezibot-android-sdk:1.0.0")
}
```

> **Note**: Replace `1.0.0` with the latest version available.

## Permissions

The SDK requires the following permissions, which are automatically merged from the library's manifest:

| Permission | Purpose |
|------------|---------|
| `INTERNET` | WebSocket communication |
| `ACCESS_NETWORK_STATE` | Network state monitoring |
| `CHANGE_NETWORK_STATE` | Network configuration |
| `CHANGE_WIFI_STATE` | WiFi hotspot creation |
| `ACCESS_WIFI_STATE` | WiFi state monitoring |
| `ACCESS_FINE_LOCATION` | Required for WiFi hotspot on Android 8+ |
| `ACCESS_COARSE_LOCATION` | Required for WiFi hotspot on Android 8+ |
| `NEARBY_WIFI_DEVICES` | Required for WiFi on Android 13+ |
| `BLUETOOTH` | BLE operations (Android ≤11) |
| `BLUETOOTH_ADMIN` | BLE operations (Android ≤11) |
| `BLUETOOTH_ADVERTISE` | BLE advertising (Android 12+) |
| `BLUETOOTH_CONNECT` | BLE connections (Android 12+) |

### Runtime Permission Handling

You must request dangerous permissions at runtime before starting the SDK:

```kotlin
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted, safe to start hosting
            dezibot.startHosting()
        } else {
            // Handle permission denial
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Android 12+ requires explicit BLE permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Android 13+ requires NEARBY_WIFI_DEVICES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}
```

---

## Quick Start

### Basic Usage

```kotlin
import de.htwk.dezibot.Dezibot

class MainActivity : AppCompatActivity() {

    private lateinit var dezibot: Dezibot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the SDK
        dezibot = Dezibot(context = this)

        // Set up callbacks
        dezibot.onStatusChange = { status ->
            Log.d("Dezibot", "Status: $status")
        }

        dezibot.onHotspotReady = { ssid, password ->
            Log.d("Dezibot", "Hotspot ready: $ssid / $password")
        }

        dezibot.onDeviceConnected = { clientId ->
            Log.d("Dezibot", "Device connected: $clientId")
        }

        dezibot.onDeviceDisconnected = { clientId ->
            Log.d("Dezibot", "Device disconnected: $clientId")
        }

        dezibot.onMessageReceived = { clientId, message ->
            Log.d("Dezibot", "Message from $clientId: $message")
        }

        // Start hosting (after permissions are granted)
        dezibot.startHosting()
    }

    override fun onDestroy() {
        super.onDestroy()
        dezibot.stopHosting()
    }
}
```

### Custom WebSocket Port

```kotlin
// Use a custom port (default is 8765)
val dezibot = Dezibot(context = this, port = 9000)
```

### Observing State with Flows

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// Observe connected devices
lifecycleScope.launch {
    dezibot.connectedDevices.collect { devices ->
        devices.forEach { device ->
            Log.d("Dezibot", "Device: ${device.displayName} (${device.ip}) - Active: ${device.isActive}")
        }
    }
}

// Observe hosting state
lifecycleScope.launch {
    dezibot.hostingState.collect { state ->
        when (state) {
            Dezibot.HostingState.Idle -> { /* Not hosting */ }
            Dezibot.HostingState.Starting -> { /* Starting up */ }
            Dezibot.HostingState.Running -> { /* Hosting active */ }
        }
    }
}

// Observe sensor readings
lifecycleScope.launch {
    dezibot.sensorReadings.collect { readings ->
        // Map<ClientID, Map<SensorType, Value>>
        readings.forEach { (clientId, sensors) ->
            sensors.forEach { (sensorType, value) ->
                Log.d("Dezibot", "$clientId - $sensorType: $value")
            }
        }
    }
}
```

---

## API Reference

### Lifecycle Methods

| Method | Description |
|--------|-------------|
| `startHosting()` | Starts the WiFi hotspot, WebSocket server, and BLE credential advertising |
| `stopHosting()` | Stops all services and disconnects all devices |
| `getConnectedDeviceCount()` | Returns the number of currently connected devices |
| `hasConnectedDevices()` | Returns `true` if any devices are connected |

### Callbacks

| Callback | Parameters | Description |
|----------|------------|-------------|
| `onStatusChange` | `(String)` | Called when the SDK status changes (for logging/UI) |
| `onHotspotReady` | `(ssid: String, password: String)` | Called when the hotspot is ready with credentials |
| `onDeviceConnected` | `(clientId: String)` | Called when a Dezibot device connects |
| `onDeviceDisconnected` | `(clientId: String)` | Called when a Dezibot device disconnects |
| `onMessageReceived` | `(clientId: String, message: String)` | Called when a message is received from a device |
| `onCredentialsRead` | `(deviceAddress: String)` | Called when a device reads credentials via BLE |
| `onBleError` | `(reason: String)` | Called when BLE advertising fails |

### StateFlows

| Flow | Type | Description |
|------|------|-------------|
| `connectedDevices` | `StateFlow<List<ConnectedDevice>>` | List of currently connected devices |
| `hostingState` | `StateFlow<HostingState>` | Current hosting state (Idle, Starting, Running) |
| `sensorReadings` | `StateFlow<Map<String, Map<String, String>>>` | Sensor readings per device (ClientID → SensorType → Value) |

---

## Motion Commands

Control the Dezibot's movement with these methods:

```kotlin
// Move forward continuously
dezibot.move()

// Move forward for 2 seconds
dezibot.move(durationMs = 2000)

// Move forward with custom motor power (PWM duty)
dezibot.move(durationMs = 1000, baseValue = 4500)

// Rotate clockwise
dezibot.rotateClockwise()
dezibot.rotateClockwise(durationMs = 500)

// Rotate counter-clockwise
dezibot.rotateAntiClockwise()
dezibot.rotateAntiClockwise(durationMs = 500)

// Stop all movement
dezibot.stop()
```

### Motion Method Reference

| Method | Parameters | Description |
|--------|------------|-------------|
| `move()` | `durationMs: Long = 0`, `baseValue: Int = 3900` | Move forward. `0` = continuous until `stop()` |
| `rotateClockwise()` | `durationMs: Long = 0`, `baseValue: Int = 3900` | Rotate clockwise |
| `rotateAntiClockwise()` | `durationMs: Long = 0`, `baseValue: Int = 3900` | Rotate counter-clockwise |
| `stop()` | - | Stop all movement immediately |

---

## Display Commands

Control the Dezibot's OLED display:

```kotlin
// Print text (no line break)
dezibot.print("Hello")

// Print text with line break
dezibot.println("Hello World")

// Clear the display
dezibot.clearDisplay()

// Flip display orientation by 180°
dezibot.flipOrientation()

// Invert display colors (call again to revert)
dezibot.invertColor()

// Identify all connected devices (each shows its name)
dezibot.identifyDevices()
```

### Display Method Reference

| Method | Parameters | Description |
|--------|------------|-------------|
| `print()` | `text: String` | Print text without line break |
| `println()` | `text: String` | Print text with line break |
| `clearDisplay()` | - | Clear all content from display |
| `flipOrientation()` | - | Flip display 180° (toggle) |
| `invertColor()` | - | Invert display colors (toggle) |
| `identifyDevices()` | - | Show each device's name on its display |

---

## LED Commands (MultiColorLight)

Control the RGB LEDs on the Dezibot. The Dezibot has three LEDs:
- **TOP_LEFT**: Left LED (when robot faces away from you)
- **TOP_RIGHT**: Right LED (when robot faces away from you)
- **BOTTOM**: Bottom LED

### LED Positions

```kotlin
import de.htwk.dezibot.data.requests.led.LedPosition

LedPosition.TOP_LEFT   // Left top LED
LedPosition.TOP_RIGHT  // Right top LED
LedPosition.BOTTOM     // Bottom LED
LedPosition.TOP        // Both top LEDs
LedPosition.ALL        // All three LEDs
```

### LED Examples

```kotlin
// Set a specific LED to a color (RGB values: 0-100)
dezibot.setLed(LedPosition.TOP_LEFT, red = 100, green = 0, blue = 0)

// Set both top LEDs to green
dezibot.setTopLeds(red = 0, green = 100, blue = 0)

// Blink an LED
dezibot.blink(
    position = LedPosition.ALL,
    amount = 5  // Blink 5 times
)

// Blink with custom color (hex format: 0x00RRGGBB) and interval
dezibot.blink(
    position = LedPosition.TOP,
    amount = 3,
    color = 0x00FF0000,  // Red
    interval = 100       // 100ms on per blink
)

// Turn off LEDs
dezibot.turnOffLeds()                        // Turn off all LEDs
dezibot.turnOffLeds(LedPosition.BOTTOM)      // Turn off specific LED
```

### LED Method Reference

| Method | Parameters | Description |
|--------|------------|-------------|
| `setLed()` | `position: LedPosition`, `red: Int`, `green: Int`, `blue: Int` | Set LED color (RGB 0-100) |
| `setTopLeds()` | `red: Int`, `green: Int`, `blue: Int` | Set both top LEDs |
| `blink()` | `position: LedPosition`, `amount: Int`, `color: Long? = null`, `interval: Int? = null` | Blink LEDs |
| `turnOffLeds()` | `position: LedPosition? = null` | Turn off LEDs (null = all) |

---

## Light Detection Commands

Read values from the Dezibot's light sensors.

### Light Sensors

```kotlin
import de.htwk.dezibot.data.requests.light.LightSensor
import de.htwk.dezibot.data.requests.light.LightSensorType

// Infrared sensors
LightSensor.IR_LEFT
LightSensor.IR_RIGHT
LightSensor.IR_FRONT
LightSensor.IR_BACK

// Daylight sensors
LightSensor.DL_FRONT
LightSensor.DL_BOTTOM

// Sensor types (for getBrightest)
LightSensorType.IR        // Infrared sensors
LightSensorType.DAYLIGHT  // Daylight sensors
```

### Light Detection Examples

```kotlin
// Get value from a specific sensor
dezibot.getLightValue(LightSensor.IR_FRONT)

// Get the brightest sensor of a type
dezibot.getBrightestSensor(LightSensorType.IR)

// Get average value over multiple measurements
dezibot.getAverageLightValue(
    sensor = LightSensor.DL_FRONT,
    measurements = 10,    // Number of measurements
    timeBetween = 5       // Milliseconds between measurements
)
```

### Light Detection Method Reference

| Method | Parameters | Description |
|--------|------------|-------------|
| `getLightValue()` | `sensor: LightSensor` | Get current value from a sensor |
| `getBrightestSensor()` | `type: LightSensorType` | Get which sensor of the type is brightest |
| `getAverageLightValue()` | `sensor: LightSensor`, `measurements: Int = 10`, `timeBetween: Int = 5` | Get average over multiple readings |

---

## Color Detection Commands

Read color values from the VEML6040 color sensor.

### Color Channels and Configuration

```kotlin
import de.htwk.dezibot.data.requests.color.ColorChannel
import de.htwk.dezibot.data.requests.color.ColorSensorMode
import de.htwk.dezibot.data.requests.color.ExposureTime

// Color channels
ColorChannel.RED
ColorChannel.GREEN
ColorChannel.BLUE
ColorChannel.WHITE

// Sensor modes
ColorSensorMode.AUTO
ColorSensorMode.MANUAL

// Exposure times (integration time)
ExposureTime.MS_40
ExposureTime.MS_80
ExposureTime.MS_160
ExposureTime.MS_320
ExposureTime.MS_640
ExposureTime.MS_1280
```

### Color Detection Examples

```kotlin
// Configure the color sensor
dezibot.configureColorSensor(
    mode = ColorSensorMode.AUTO,
    enabled = true,
    exposureTime = ExposureTime.MS_160
)

// Start auto mode (required before reading values)
dezibot.beginColorAutoMode()

// Get a color channel value (suspending function)
lifecycleScope.launch {
    dezibot.getColorValue(ColorChannel.RED)
    dezibot.getColorValue(ColorChannel.GREEN)
    dezibot.getColorValue(ColorChannel.BLUE)
}

// Get ambient light in lux (suspending function)
lifecycleScope.launch {
    dezibot.getAmbientLight()
}
```

> **Note**: `getColorValue()` and `getAmbientLight()` are `suspend` functions that automatically ensure the sensor is in auto mode before reading.

### Color Detection Method Reference

| Method | Parameters | Description |
|--------|------------|-------------|
| `configureColorSensor()` | `mode: ColorSensorMode`, `enabled: Boolean`, `exposureTime: ExposureTime` | Configure the color sensor |
| `beginColorAutoMode()` | - | Start auto mode for color sensor |
| `getColorValue()` | `channel: ColorChannel` | Get color channel value (0-65535) |
| `getAmbientLight()` | - | Get ambient light level in lux |

---

## Device Management

### Selective Device Control

You can enable or disable individual devices to control which devices receive commands:

```kotlin
// Get the list of connected devices
val devices = dezibot.connectedDevices.value

// Disable a specific device (won't receive broadcast commands)
dezibot.setDeviceActive(devices[0], isActive = false)

// Re-enable the device
dezibot.setDeviceActive(devices[0], isActive = true)
```

### ConnectedDevice Properties

```kotlin
data class ConnectedDevice(
    val ip: String,           // IP address of the device
    val clientId: String,     // Unique client identifier
    val displayName: String?, // Device's randomly generated name
    val isActive: Boolean     // Whether device receives commands
)
```

---

## Architecture Overview

### Connection Flow

```
┌─────────────────────┐                      ┌─────────────────────┐
│   Android Device    │                      │      Dezibot        │
│   (SDK Host)        │                      │      (ESP32)        │
└─────────────────────┘                      └─────────────────────┘
         │                                            │
         │ 1. Start WiFi Hotspot                      │
         │ 2. Start WebSocket Server                  │
         │ 3. Start BLE GATT Advertising              │
         │                                            │
         │◄────────────────────────────────────────────
         │         4. BLE Scan finds service          │
         │                                            │
         │◄────────────────────────────────────────────
         │         5. Connect to GATT Server          │
         │                                            │
         ├───────────────────────────────────────────►│
         │         6. Return SSID, Password, Port     │
         │                                            │
         │◄────────────────────────────────────────────
         │         7. Disconnect BLE                  │
         │                                            │
         │◄────────────────────────────────────────────
         │         8. Connect to WiFi Hotspot         │
         │                                            │
         │◄════════════════════════════════════════════
         │         9. WebSocket Connection            │
         │            (Bidirectional)                 │
         ╘════════════════════════════════════════════╛
```

### BLE GATT Service

The SDK advertises a BLE GATT service for credential sharing:

| Characteristic | UUID | Description |
|----------------|------|-------------|
| **Service** | `0000ff01-0000-1000-8000-00805f9b34fb` | Main service UUID |
| **SSID** | `0000ff02-0000-1000-8000-00805f9b34fb` | Hotspot SSID |
| **Password** | `0000ff03-0000-1000-8000-00805f9b34fb` | Hotspot password |
| **Server Port** | `0000ff05-0000-1000-8000-00805f9b34fb` | WebSocket server port |

### WebSocket Protocol

Commands are sent as JSON messages with the following structure:

```json
{
  "action": "set",
  "target": "motion",
  "command": "move",
  "durationMs": 1000,
  "baseValue": 3900
}
```

| Field | Values | Description |
|-------|--------|-------------|
| `action` | `set`, `get` | Action type |
| `target` | `motion`, `display`, `color`, `light`, `multiColorLight` | Target component |
| `command` | (varies) | Specific command name |

Responses include a `status` field:

```json
{
  "status": "ok",
  "target": "light",
  "command": "getValue",
  "sensor": "IR_FRONT",
  "value": 1234
}
```

---

## Troubleshooting

### Common Issues

**Hotspot fails to start**
- Ensure location permissions are granted
- Check that WiFi is enabled on the device
- Some devices don't support LocalOnlyHotspot API

**BLE advertising fails**
- Ensure Bluetooth is enabled
- On Android 12+, `BLUETOOTH_ADVERTISE` permission is required
- Some devices don't support BLE peripheral mode

**Devices not connecting**
- **Verify the Dezibot has the required WebSocket firmware** (see [Firmware Requirement](#️-firmware-requirement))
- Check that the Dezibot is scanning for the correct BLE service UUID
- Ensure the device is within WiFi range

**Commands not received**
- Check if the device is marked as active: `device.isActive`
- Verify WebSocket connection via `onDeviceConnected` callback
- Use `onMessageReceived` to debug responses

---

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.


