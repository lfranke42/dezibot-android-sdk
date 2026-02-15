package de.htwk.dezibot.internal

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import de.htwk.dezibot.data.models.ConnectedDevice
import de.htwk.dezibot.data.requests.DezibotRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress


/**
 * WebSocket server that accepts connections from ESP32 devices.
 * Runs on the Android device and broadcasts commands to all connected clients.
 */
@PublishedApi
internal class DezibotWebSocketServer(
    port: Int = DEFAULT_PORT,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WebSocketServer(InetSocketAddress(port)) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())


    init {
        // Allow the port to be rebound immediately after the server stops,
        // preventing "Address already in use" errors on restart.
        isReuseAddr = true
        connectionLostTimeout = 10
    }

    @PublishedApi
    internal val moshi: Moshi = Moshi.Builder().build()

    interface Listener {
        fun onClientConnected(clientId: String)
        fun onClientDisconnected(clientId: String)
        fun onMessageReceived(clientId: String, message: String)
        fun onError(t: Throwable)
    }

    var listener: Listener? = null

    private val connectedClients = mutableMapOf<String, WebSocket>()
    private val displayNames = mutableMapOf<String, String>()
    private val disabledNames = mutableSetOf<String>()

    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())

    /**
     * A flow of currently connected devices.
     * Emits a new list whenever a client connects or disconnects,
     * including ungraceful disconnects detected by the ping/pong timeout.
     */
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val clientId = conn.remoteSocketAddress.toString()
        // Store clientId as attachment so it's available in onClose
        // even when remoteSocketAddress returns null (ungraceful disconnect).
        conn.setAttachment(clientId)
        connectedClients[clientId] = conn
        emitConnectedDevices()
        listener?.onClientConnected(clientId)
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val clientId: String = conn.getAttachment() ?: "unknown"
        connectedClients.remove(clientId)
        emitConnectedDevices()
        listener?.onClientDisconnected(clientId)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val clientId: String = conn.getAttachment() ?: conn.remoteSocketAddress.toString()
        listener?.onMessageReceived(clientId, message)
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        listener?.onError(ex)
    }

    override fun onStart() {
        // Server started successfully
    }

    /**
     * Gracefully stop the server: close all client connections, then stop
     * with a timeout to ensure the port is fully released.
     */
    fun stopGracefully(timeoutMs: Int = 1000) {
        try {
            // Close all active client connections
            for ((_, conn) in connectedClients) {
                try {
                    conn.close(CLOSE_NORMAL, "Server shutting down")
                } catch (_: Exception) { /* best effort */
                }
            }
            connectedClients.clear()
            emitConnectedDevices()

            // Stop the server with a timeout so the port is released
            stop(timeoutMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (_: Exception) {
            // Best effort cleanup
        }
    }

    /**
     * Broadcast a message to all connected clients.
     */
    fun broadcastMessage(json: String) {
        broadcast(json)
    }

    /**
     * Send a message to a specific client.
     */
    fun sendToClient(clientId: String, json: String) {
        connectedClients[clientId]?.send(json)
    }

    /**
     * Send a typed Dezibot request to all connected clients.
     * Uses Moshi to serialize the request to JSON.
     */
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : DezibotRequest> send(request: T) {
        val adapter = moshi.adapter<T>()
        val json = adapter.toJson(request)
        broadcastMessage(json)
    }

    /**
     * Send a typed Dezibot request to a specific client.
     * Uses Moshi to serialize the request to JSON.
     */
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : DezibotRequest> sendToClient(clientId: String, request: T) {
        val adapter = moshi.adapter<T>()
        val json = adapter.toJson(request)
        sendToClient(clientId, json)
    }

    /**
     * Send a typed Dezibot request only to active clients.
     * Active clients are those whose display name is NOT in the disabled list.
     * If a client has no display name, it is considered active by default.
     */
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : DezibotRequest> sendToActive(request: T) {
        val adapter = moshi.adapter<T>()
        val json = adapter.toJson(request)
        doSendToActive(json)
    }

    @PublishedApi
    internal fun doSendToActive(json: String) {
        val currentClients = connectedClients.toMap()
        for ((clientId, conn) in currentClients) {
            val displayName = displayNames[clientId]
            // If device has a name and is disabled, skip it.
            // If device has no name or is not disabled, send.
            if (displayName != null && displayName in disabledNames) {
                continue
            }
            scope.launch {
                runCatching {
                    conn.send(json)
                }.onFailure {
                    listener?.onError(it)
                }
            }
        }
    }

    /**
     * Set whether a device (identified by its display name) is active.
     * If [isActive] is false, the name is added to the disabled list.
     * If [isActive] is true, the name is removed from the disabled list.
     */
    fun setDeviceActive(displayName: String, isActive: Boolean) {
        if (isActive) {
            disabledNames.remove(displayName)
        } else {
            disabledNames.add(displayName)
        }
        emitConnectedDevices()
    }

    /**
     * Update the display name of a connected device and emit the updated list.
     */
    fun updateDeviceDisplayName(clientId: String, displayName: String) {
        displayNames[clientId] = displayName
        emitConnectedDevices()
    }

    /**
     * Get the number of connected clients.
     */
    fun getConnectedClientCount(): Int = connectedClients.size

    /**
     * Check if any clients are connected.
     */
    fun hasConnectedClients(): Boolean = connectedClients.isNotEmpty()

    /**
     * Rebuilds the [_connectedDevices] list from the current [connectedClients] map
     * and emits it to collectors.
     */
    private fun emitConnectedDevices() {
        _connectedDevices.value = connectedClients.keys.map { clientId ->
            val (ip, _) = parseClientId(clientId)
            val name = displayNames[clientId]
            // Device is active if it has no name (yet) OR if its name is not in the disabled list
            val isActive = name == null || name !in disabledNames
            ConnectedDevice(ip = ip, clientId = clientId, displayName = name, isActive = isActive)
        }
    }

    /**
     * Returns the client IDs of all currently connected clients.
     */
    fun getConnectedClientIds(): Set<String> = connectedClients.keys.toSet()

    companion object {
        const val DEFAULT_PORT = 8765
        private const val CLOSE_NORMAL = 1000
    }
}
