package de.htwk.dezibot

import android.content.Context
import android.util.Log
import de.htwk.dezibot.data.models.ConnectedDevice
import de.htwk.dezibot.data.requests.color.BeginAutoModeRequest
import de.htwk.dezibot.data.requests.color.ColorChannel
import de.htwk.dezibot.data.requests.color.ColorSensorMode
import de.htwk.dezibot.data.requests.color.ConfigureColorRequest
import de.htwk.dezibot.data.requests.color.ExposureTime
import de.htwk.dezibot.data.requests.color.GetAmbientLightRequest
import de.htwk.dezibot.data.requests.color.GetColorValueRequest
import de.htwk.dezibot.data.requests.display.ClearRequest
import de.htwk.dezibot.data.requests.display.FlipOrientationRequest
import de.htwk.dezibot.data.requests.display.InvertColorRequest
import de.htwk.dezibot.data.requests.display.PrintRequest
import de.htwk.dezibot.data.requests.display.PrintlnRequest
import de.htwk.dezibot.data.requests.display.ShowNameRequest
import de.htwk.dezibot.data.requests.led.BlinkByPositionRequest
import de.htwk.dezibot.data.requests.led.LedPosition
import de.htwk.dezibot.data.requests.led.SetLedByPositionRgbRequest
import de.htwk.dezibot.data.requests.led.SetTopLedsRgbRequest
import de.htwk.dezibot.data.requests.led.TurnOffLedsRequest
import de.htwk.dezibot.data.requests.light.GetAverageValueRequest
import de.htwk.dezibot.data.requests.light.GetBrightestRequest
import de.htwk.dezibot.data.requests.light.GetValueRequest
import de.htwk.dezibot.data.requests.light.LightSensor
import de.htwk.dezibot.data.requests.light.LightSensorType
import de.htwk.dezibot.data.requests.motion.MoveRequest
import de.htwk.dezibot.data.requests.motion.RotateAntiClockwiseRequest
import de.htwk.dezibot.data.requests.motion.RotateClockwiseRequest
import de.htwk.dezibot.data.requests.motion.StopRequest
import de.htwk.dezibot.data.responses.color.BeginAutoModeResponse
import de.htwk.dezibot.data.responses.color.GetAmbientLightResponse
import de.htwk.dezibot.data.responses.color.GetColorValueResponse
import de.htwk.dezibot.data.responses.display.ShowNameResponse
import de.htwk.dezibot.data.responses.light.GetBrightestResponse
import de.htwk.dezibot.data.responses.light.GetValueResponse
import de.htwk.dezibot.internal.DezibotBleCredentialSharer
import de.htwk.dezibot.internal.DezibotHotspotManager
import de.htwk.dezibot.internal.DezibotWebSocketServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Main entry point for controlling Dezibot devices.
 *
 * This class creates a WiFi hotspot and WebSocket server, then advertises the
 * hotspot credentials via BLE so Dezibot devices can discover and connect.
 *
 * Usage:
 * 1. Create a Dezibot instance
 * 2. Set callback handlers (onStatusChange, onHotspotReady, etc.)
 * 3. Call startHosting() to start the hotspot, server, and BLE credential advertising
 * 4. Dezibot devices discover credentials via BLE and connect automatically
 * 5. Dezibot devices connect to the hotspot and WebSocket server at 192.168.43.1:${port}
 * 6. Send commands using the various command methods
 * 7. Call stopHosting() when done
 *
 * @param context Android context (usually Activity or Application context)
 * @param port WebSocket server port (default: 8765)
 */
class Dezibot(
    private val context: Context,
    private val port: Int = DezibotWebSocketServer.DEFAULT_PORT
) {

    private val hotspotManager = DezibotHotspotManager(context)
    private val bleCredentialSharer = DezibotBleCredentialSharer(context)
    private var webSocketServer: DezibotWebSocketServer? = null
    private var deviceCollectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())

    /**
     * A flow emitting the list of currently connected Dezibot devices.
     */
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _sensorReadings = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())

    /**
     * A flow emitting the latest sensor readings for each connected device.
     * Map<ClientID, Map<SensorType, Value>>
     */
    val sensorReadings: StateFlow<Map<String, Map<String, String>>> = _sensorReadings.asStateFlow()

    enum class HostingState {
        Idle,
        Starting,
        Running
    }

    private val _hostingState = MutableStateFlow(HostingState.Idle)

    private val _messageFlow = MutableSharedFlow<Pair<String, String>>()
    private val messageFlow = _messageFlow.asSharedFlow()

    /**
     * A flow emitting the current state of the hotspot and server.
     */
    val hostingState: StateFlow<HostingState> = _hostingState.asStateFlow()

    /** Called when the status changes (for logging/UI updates) */
    var onStatusChange: ((String) -> Unit)? = null

    /** Called when the hotspot is ready with the SSID and password */
    var onHotspotReady: ((ssid: String, password: String) -> Unit)? = null

    /** Called when a Dezibot device connects */
    var onDeviceConnected: ((clientId: String) -> Unit)? = null

    /** Called when a Dezibot device disconnects */
    var onDeviceDisconnected: ((clientId: String) -> Unit)? = null

    /** Called when a message is received from a Dezibot device */
    var onMessageReceived: ((clientId: String, message: String) -> Unit)? = null

    /** Called when a Dezibot reads credentials via BLE */
    var onCredentialsRead: ((deviceAddress: String) -> Unit)? = null

    /** Called when BLE credential advertising fails */
    var onBleError: ((reason: String) -> Unit)? = null

    init {
        hotspotManager.listener = object : DezibotHotspotManager.Listener {
            override fun onHotspotStarted(ssid: String, password: String) {
                _hostingState.value = HostingState.Running
                onStatusChange?.invoke("Hotspot ready: $ssid")
                onHotspotReady?.invoke(ssid, password)

                onStatusChange?.invoke("Starting WebSocket server on port $port...")
                scope.launch {
                    try {
                        webSocketServer?.start()
                        onStatusChange?.invoke("Server running")
                    } catch (e: Exception) {
                        onStatusChange?.invoke("Server error: ${e.message}")
                    }
                }

                startBleAdvertising(ssid, password)
            }

            override fun onHotspotStopped() {
                _hostingState.value = HostingState.Idle
                onStatusChange?.invoke("Hotspot stopped")
            }

            override fun onHotspotFailed(reason: String) {
                _hostingState.value = HostingState.Idle
                onStatusChange?.invoke("Hotspot failed: $reason")
            }
        }

        bleCredentialSharer.listener = object : DezibotBleCredentialSharer.Listener {
            override fun onCredentialsRead(deviceAddress: String) {
                onStatusChange?.invoke("Dezibot $deviceAddress read credentials via BLE")
                this@Dezibot.onCredentialsRead?.invoke(deviceAddress)
            }

            override fun onBleError(reason: String) {
                onStatusChange?.invoke("BLE error: $reason")
                this@Dezibot.onBleError?.invoke(reason)
            }
        }
    }

    /**
     * Creates a new WebSocket server instance with listeners attached.
     * Must be called before each startHosting() since WebSocketServer can only be started once.
     */
    private fun createWebSocketServer(): DezibotWebSocketServer {
        return DezibotWebSocketServer(port).apply {
            listener = object : DezibotWebSocketServer.Listener {
                override fun onClientConnected(clientId: String) {
                    onStatusChange?.invoke("Device connected: $clientId")
                    onDeviceConnected?.invoke(clientId)
                    webSocketServer?.sendToClient(clientId, ShowNameRequest())
                }

                override fun onClientDisconnected(clientId: String) {
                    onStatusChange?.invoke("Device disconnected: $clientId")
                    onDeviceDisconnected?.invoke(clientId)
                }

                override fun onMessageReceived(clientId: String, message: String) {
                    onStatusChange?.invoke("Received from $clientId: $message")
                    handleShowNameResponse(clientId, message)
                    handleSensorResponse(clientId, message)
                    this@Dezibot.onMessageReceived?.invoke(clientId, message)
                    scope.launch {
                        _messageFlow.emit(clientId to message)
                    }
                }

                override fun onError(t: Throwable) {
                    onStatusChange?.invoke("Error: ${t.message}")
                }
            }

            // Pipe the server's connected-devices flow into Dezibot's public StateFlow
            deviceCollectJob = scope.launch {
                connectedDevices.collect { devices ->
                    _connectedDevices.value = devices
                }
            }
        }
    }

    /**
     * Start hosting - creates the WiFi hotspot, starts the WebSocket server,
     * and begins BLE credential advertising for Dezibot devices.
     *
     * Dezibot devices should:
     * 1. Scan for the BLE GATT service (UUID: 0000ff01-0000-1000-8000-00805f9b34fb)
     * 2. Connect and read SSID, password, server IP, and port characteristics
     * 3. Connect to the Android WiFi hotspot using the received credentials
     * 4. Connect to WebSocket at ws://<serverIp>:<serverPort>/
     *
     * Requires Android 8.0+ (API 26+), CHANGE_WIFI_STATE + location + BLE permissions.
     */
    fun startHosting() {
        if (_hostingState.value != HostingState.Idle) return
        _hostingState.value = HostingState.Starting
        onStatusChange?.invoke("Starting hotspot...")
        // Create a fresh server instance (WebSocketServer can only be started once)
        webSocketServer = createWebSocketServer()
        hotspotManager.startHotspot()
    }

    /**
     * Stop hosting - stops BLE advertising, WebSocket server, and turns off the hotspot.
     */
    fun stopHosting() {
        // Stop BLE credential advertising
        bleCredentialSharer.stopAdvertising()

        // Cancel the device-list collection before stopping the server
        deviceCollectJob?.cancel()
        deviceCollectJob = null

        try {
            webSocketServer?.stopGracefully()
            webSocketServer = null
        } catch (e: Exception) {
            Log.e("Dezibot", "Error stopping server: ${e.message}")
        }
        hotspotManager.stopHotspot()
        _connectedDevices.value = emptyList()
        _hostingState.value = HostingState.Idle
        onStatusChange?.invoke("Stopped")
    }

    /**
     * Get the number of connected Dezibot devices.
     */
    fun getConnectedDeviceCount(): Int = webSocketServer?.getConnectedClientCount() ?: 0

    /**
     * Check if any Dezibot devices are connected.
     */
    fun hasConnectedDevices(): Boolean = webSocketServer?.hasConnectedClients() ?: false

    // ==================== DEVICE MANAGEMENT ====================

    /**
     * Set whether a specific device is active.
     * When inactive, a device will not receive broadcast commands.
     * The setting is persisted by device name, so it survives reconnections.
     *
     * @param device The device to configure
     * @param isActive True to enable commands, false to disable
     */
    fun setDeviceActive(device: ConnectedDevice, isActive: Boolean) {
        device.displayName?.let { name ->
            webSocketServer?.setDeviceActive(name, isActive)
        }
    }

    // ==================== BLE CREDENTIAL SHARING ====================

    /**
     * Start BLE GATT advertising with the hotspot credentials.
     * Called automatically when the hotspot starts.
     */
    private fun startBleAdvertising(ssid: String, password: String) {
        onStatusChange?.invoke("Starting BLE credential advertising...")
        bleCredentialSharer.startAdvertising(
            ssid = ssid,
            password = password,
            serverPort = port
        )
    }

    // ==================== MOTION COMMANDS ====================

    /**
     * Move the robot forward.
     * @param durationMs Duration in milliseconds, 0 = continuous until stop
     * @param baseValue PWM base duty (default: 3900)
     */
    fun move(durationMs: Long = 0, baseValue: Int = 3900) {
        webSocketServer?.sendToActive(MoveRequest(durationMs = durationMs, baseValue = baseValue))
    }

    /**
     * Rotate the robot clockwise.
     */
    fun rotateClockwise(durationMs: Long = 0, baseValue: Int = 3900) {
        webSocketServer?.sendToActive(
            RotateClockwiseRequest(
                durationMs = durationMs,
                baseValue = baseValue
            )
        )
    }

    /**
     * Rotate the robot counter-clockwise.
     */
    fun rotateAntiClockwise(durationMs: Long = 0, baseValue: Int = 3900) {
        webSocketServer?.sendToActive(
            RotateAntiClockwiseRequest(
                durationMs = durationMs,
                baseValue = baseValue
            )
        )
    }

    /**
     * Stop all motion.
     */
    fun stop() {
        webSocketServer?.sendToActive(StopRequest())
    }

    // ==================== DISPLAY COMMANDS ====================

    /**
     * Print text on the display.
     */
    fun print(text: String) {
        webSocketServer?.sendToActive(PrintRequest(text = text))
    }

    /**
     * Print text with a line break.
     */
    fun println(text: String) {
        webSocketServer?.sendToActive(PrintlnRequest(text = text))
    }

    /**
     * Clear the display.
     */
    fun clearDisplay() {
        webSocketServer?.sendToActive(ClearRequest())
    }

    /**
     * Flip display orientation by 180°.
     */
    fun flipOrientation() {
        webSocketServer?.sendToActive(FlipOrientationRequest())
    }

    /**
     * Invert display colors.
     */
    fun invertColor() {
        webSocketServer?.sendToActive(InvertColorRequest())
    }

    /**
     * Identify all connected Dezibot devices by sending the showName command to each.
     * Each Dezibot will display its name on screen and return it in the response,
     * which is then associated with the device's IP address.
     */
    fun identifyDevices() {
        val server = webSocketServer ?: return
        val request = ShowNameRequest()
        for (clientId in server.getConnectedClientIds()) {
            server.sendToClient(clientId, request)
        }
    }

    /**
     * Parses a showName response and updates the device display name.
     */
    private fun handleShowNameResponse(clientId: String, message: String) {
        runCatching {
            val moshi = webSocketServer?.moshi ?: return
            val adapter = moshi.adapter(ShowNameResponse::class.java)
            val response = adapter.fromJson(message) ?: return
            if (response.command == "showName" && response.status == de.htwk.dezibot.data.responses.Status.OK) {
                webSocketServer?.updateDeviceDisplayName(clientId, response.dezibot)
            }
        }
    }

    private fun handleSensorResponse(clientId: String, message: String) {
        val moshi = webSocketServer?.moshi ?: return

        runCatching {
            // Try parsing as GetColorValueResponse
            val colorAdapter = moshi.adapter(GetColorValueResponse::class.java)
            val colorResponse = colorAdapter.fromJson(message)
            if (colorResponse?.command == "getColorValue" && colorResponse.status == de.htwk.dezibot.data.responses.Status.OK) {
                // Ensure color is uppercase to match SensorOption keys (e.g. "RED" -> "Color_RED")
                val colorKey = colorResponse.color?.uppercase() ?: "UNKNOWN"
                updateSensorReading(clientId, "Color_$colorKey", colorResponse.value.toString())
                return
            }

            // Try parsing as BeginAutoModeResponse
            val autoModeAdapter = moshi.adapter(BeginAutoModeResponse::class.java)
            val autoModeResponse = autoModeAdapter.fromJson(message)
            if (autoModeResponse?.command == "beginAutoMode" && autoModeResponse.status == de.htwk.dezibot.data.responses.Status.OK) {
                updateSensorReading(
                    clientId,
                    "Auto_Color_Mode",
                    if (autoModeResponse.started == true) "Started" else "Stopped"
                )
                return
            }

            // Try parsing as GetAmbientLightResponse
            val ambientAdapter = moshi.adapter(GetAmbientLightResponse::class.java)
            val ambientResponse = ambientAdapter.fromJson(message)
            if (ambientResponse?.command == "getAmbientLight" && ambientResponse.status == de.htwk.dezibot.data.responses.Status.OK) {
                updateSensorReading(clientId, "Ambient Light", "${ambientResponse.lux} lux")
                return
            }

            // Try parsing as GetValueResponse (Light)
            val lightAdapter = moshi.adapter(GetValueResponse::class.java)
            val lightResponse = lightAdapter.fromJson(message)
            if (lightResponse?.command == "getValue" && lightResponse.status == de.htwk.dezibot.data.responses.Status.OK) {
                updateSensorReading(
                    clientId,
                    "Light_${lightResponse.sensor}",
                    lightResponse.value.toString()
                )
                return
            }

            // Try parsing as GetBrightestResponse
            val brightestAdapter = moshi.adapter(GetBrightestResponse::class.java)
            val brightestResponse = brightestAdapter.fromJson(message)
            if (brightestResponse?.command == "getBrightest" && brightestResponse.status == de.htwk.dezibot.data.responses.Status.OK) {
                updateSensorReading(
                    clientId,
                    "Brightest_${brightestResponse.type}",
                    brightestResponse.sensor ?: "Unknown"
                )
                return
            }
        }
    }

    private fun updateSensorReading(clientId: String, sensorType: String, value: String) {
        Log.d("Dezibot", "updateSensorReading: Updating $clientId - $sensorType = $value")
        _sensorReadings.update { currentReadings ->
            val clientReadings = currentReadings[clientId]?.toMutableMap() ?: mutableMapOf()
            clientReadings[sensorType] = value
            currentReadings + (clientId to clientReadings)
        }
    }

    // ==================== LED COMMANDS ====================

    /**
     * Set LED color by position using RGB values.
     */
    fun setLed(position: LedPosition, red: Int, green: Int, blue: Int) {
        webSocketServer?.sendToActive(SetLedByPositionRgbRequest.create(position, red, green, blue))
    }

    /**
     * Set both top LEDs.
     */
    fun setTopLeds(red: Int, green: Int, blue: Int) {
        webSocketServer?.sendToActive(SetTopLedsRgbRequest(red = red, green = green, blue = blue))
    }

    /**
     * Blink LEDs at a position.
     */
    fun blink(position: LedPosition, amount: Int, color: Long? = null, interval: Int? = null) {
        webSocketServer?.sendToActive(
            BlinkByPositionRequest.create(
                amount,
                position,
                color,
                interval
            )
        )
    }

    /**
     * Turn off LEDs.
     */
    fun turnOffLeds(position: LedPosition? = null) {
        webSocketServer?.sendToActive(TurnOffLedsRequest.create(position))
    }

    // ==================== LIGHT DETECTION COMMANDS ====================

    /**
     * Get value from a light sensor.
     */
    fun getLightValue(sensor: LightSensor) {
        webSocketServer?.sendToActive(GetValueRequest.create(sensor))
    }

    /**
     * Get the brightest sensor of a type.
     */
    fun getBrightestSensor(type: LightSensorType) {
        webSocketServer?.sendToActive(GetBrightestRequest.create(type))
    }

    /**
     * Get average sensor value.
     */
    fun getAverageLightValue(sensor: LightSensor, measurements: Int = 10, timeBetween: Int = 5) {
        webSocketServer?.sendToActive(
            GetAverageValueRequest.create(
                sensor,
                measurements,
                timeBetween
            )
        )
    }

    // ==================== COLOR DETECTION COMMANDS ====================

    /**
     * Configure color sensor.
     */
    fun configureColorSensor(mode: ColorSensorMode, enabled: Boolean, exposureTime: ExposureTime) {
        webSocketServer?.sendToActive(ConfigureColorRequest.create(mode, enabled, exposureTime))
    }

    /**
     * Ensures successful switch to Auto Mode for all active devices.
     * Waits for acknowledgement from active devices.
     */
    private suspend fun ensureAutoMode() {
        // 1. Identify active clients
        val activeClients = _connectedDevices.value
            .filter { it.isActive }
            .map { it.clientId }
            .toSet()

        if (activeClients.isEmpty()) {
            return
        }

        // 2. Broadcast BeginAutoModeRequest
        beginColorAutoMode()

        // 3. Wait for responses
        val respondedClients = mutableSetOf<String>()
        val moshi = webSocketServer?.moshi ?: return
        val adapter = moshi.adapter(BeginAutoModeResponse::class.java)

        try {
            withTimeout(2000) { // 2 second timeout
                messageFlow.collect { (clientId, message) ->
                    if (clientId in activeClients && !respondedClients.contains(clientId)) {
                        runCatching {
                            val response = adapter.fromJson(message)
                            if (response?.command == "beginAutoMode" &&
                                response.status == de.htwk.dezibot.data.responses.Status.OK
                            ) {
                                respondedClients.add(clientId)
                                if (respondedClients.size == activeClients.size) {
                                    cancel()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException && respondedClients.size == activeClients.size) {
                Log.d("Dezibot", "ensureAutoMode: Success.")
            } else {
                Log.d(
                    "Dezibot",
                    "ensureAutoMode: Timeout or error. Responded: $respondedClients / Expected: $activeClients"
                )
            }
        }
    }

    /**
     * Begin auto mode for color sensor.
     */
    fun beginColorAutoMode() {
        webSocketServer?.sendToActive(BeginAutoModeRequest())
    }

    /**
     * Get a color channel value.
     */
    suspend fun getColorValue(channel: ColorChannel) {
        ensureAutoMode()
        webSocketServer?.sendToActive(GetColorValueRequest.create(channel))
    }

    /**
     * Get ambient light in lux.
     */
    suspend fun getAmbientLight() {
        ensureAutoMode()
        webSocketServer?.sendToActive(GetAmbientLightRequest())
    }
}
