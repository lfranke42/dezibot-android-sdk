package de.htwk.dezibot.internal

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Shares hotspot credentials with Dezibot devices via Bluetooth Low Energy (BLE).
 *
 * Starts a BLE GATT server that advertises a custom service. Dezibot devices discover
 * the service by its UUID, connect, and read the hotspot credentials from GATT
 * characteristics. This runs entirely independently of WiFi, so the LocalOnlyHotspot
 * remains active throughout.
 *
 * ## BLE Connection Handshake
 *
 * ```
 * Android (GATT Server)                    Dezibot (GATT Client)
 * ────────────────────                     ─────────────────────
 * 1. Start BLE advertising
 *    (service UUID in advert data)
 *                                          2. BLE scan finds service UUID
 *                                          3. Connect to GATT server
 * 4. onConnectionStateChange
 *    (CONNECTED)
 *                                          5. Discover services
 * 6. Return service list
 *                                          7. Read CHARACTERISTIC_SSID
 * 8. Return SSID value
 *                                          9. Read CHARACTERISTIC_PASSWORD
 * 10. Return password value
 *                                          11. Read CHARACTERISTIC_SERVER_PORT
 * 12. Return server port value
 *                                          13. Disconnect from GATT
 * 14. onConnectionStateChange
 *     (DISCONNECTED)
 *                                          15. Connect to WiFi hotspot
 *                                              using received credentials
 *                                          16. Connect to WebSocket at
 *                                              ws://<serverIp>:<serverPort>/
 * ```
 *
 * ## Service & Characteristic UUIDs
 *
 * | Name             | UUID                                   |
 * |------------------|----------------------------------------|
 * | Service          | `0000ff01-0000-1000-8000-00805f9b34fb`  |
 * | SSID             | `0000ff02-0000-1000-8000-00805f9b34fb`  |
 * | Password         | `0000ff03-0000-1000-8000-00805f9b34fb`  |
 * | Server Port      | `0000ff05-0000-1000-8000-00805f9b34fb`  |
 */
internal class DezibotBleCredentialSharer(private val context: Context) {

    interface Listener {
        /** Called when a Dezibot has connected and read credentials. */
        fun onCredentialsRead(deviceAddress: String)

        /** Called when BLE advertising or GATT server fails. */
        fun onBleError(reason: String)
    }

    var listener: Listener? = null

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    // Current credential values (updated when hotspot starts)
    private var ssid: String = ""
    private var password: String = ""
    private var serverPort: Int = 0

    /** Tracks which characteristics each device has read. */
    private val deviceReadProgress = mutableMapOf<String, MutableSet<String>>()

    // ==================== Public API ====================

    /**
     * Start BLE advertising and the GATT server with the given hotspot credentials.
     *
     * @param ssid Hotspot SSID
     * @param password Hotspot password
     * @param serverPort WebSocket server port
     */
    fun startAdvertising(ssid: String, password: String, serverPort: Int) {
        this.ssid = ssid
        this.password = password
        this.serverPort = serverPort

        val missingPermissions = getMissingBlePermissions()
        if (missingPermissions.isNotEmpty()) {
            val msg = "Missing BLE permissions: ${missingPermissions.joinToString()}"
            Log.w(TAG, msg)
            listener?.onBleError(msg)
            return
        }

        val manager = bluetoothManager
        if (manager == null) {
            listener?.onBleError("Bluetooth not available on this device")
            return
        }

        val adapter = manager.adapter
        if (adapter == null || !adapter.isEnabled) {
            listener?.onBleError("Bluetooth is not enabled")
            return
        }

        // Start GATT server
        startGattServer(manager)

        // Start BLE advertising
        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            listener?.onBleError("BLE advertising not supported on this device")
            stopGattServer()
            return
        }

        startBleAdvertising()
    }

    /**
     * Stop BLE advertising and shut down the GATT server.
     */
    fun stopAdvertising() {
        stopBleAdvertising()
        stopGattServer()
        deviceReadProgress.clear()
        Log.d(TAG, "BLE credential sharing stopped")
    }

    /**
     * Update the credentials being served (e.g. if hotspot restarts with new credentials).
     */
    fun updateCredentials(ssid: String, password: String, serverIp: String, serverPort: Int) {
        this.ssid = ssid
        this.password = password
        this.serverPort = serverPort
        Log.d(TAG, "Credentials updated: SSID=$ssid")
    }

    // ==================== Permission Checks ====================

    /**
     * Returns the list of missing BLE permissions, or empty if all are granted.
     *
     * The host app must request these at runtime before calling [startAdvertising]:
     * - API 31+ (Android 12+): `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`
     * - API 26–30: `BLUETOOTH`, `BLUETOOTH_ADMIN` (auto-granted at install, should not appear)
     */
    private fun getMissingBlePermissions(): List<String> {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        return required.filter { !hasPermission(it) }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    // ==================== GATT Server ====================

    private fun startGattServer(manager: BluetoothManager) {
        try {
            gattServer = manager.openGattServer(context, gattServerCallback)

            val service = BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            service.addCharacteristic(createReadCharacteristic(CHARACTERISTIC_SSID))
            service.addCharacteristic(createReadCharacteristic(CHARACTERISTIC_PASSWORD))
            service.addCharacteristic(createReadCharacteristic(CHARACTERISTIC_SERVER_PORT))

            gattServer?.addService(service)
            Log.d(TAG, "GATT server started with credential service")
        } catch (e: SecurityException) {
            listener?.onBleError("BLE permission denied: ${e.message}")
        } catch (e: Exception) {
            listener?.onBleError("Failed to start GATT server: ${e.message}")
        }
    }

    private fun createReadCharacteristic(uuid: java.util.UUID): BluetoothGattCharacteristic {
        return BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            try {
                val address = device.address
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Dezibot connected via BLE: $address")
                    deviceReadProgress[address] = mutableSetOf()
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Dezibot disconnected via BLE: $address")
                    val readChars = deviceReadProgress.remove(address)
                    if (readChars != null && readChars.size == 4) {
                        Log.d(TAG, "Dezibot $address read all credentials successfully")
                        listener?.onCredentialsRead(address)
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException in onConnectionStateChange: ${e.message}")
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            try {
                val value = when (characteristic.uuid) {
                    CHARACTERISTIC_SSID -> ssid.toByteArray(Charsets.UTF_8)
                    CHARACTERISTIC_PASSWORD -> password.toByteArray(Charsets.UTF_8)
                    CHARACTERISTIC_SERVER_PORT -> serverPort.toString().toByteArray(Charsets.UTF_8)
                    else -> {
                        gattServer?.sendResponse(
                            device, requestId,
                            android.bluetooth.BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED,
                            0, null
                        )
                        return
                    }
                }

                // Handle offset for long reads
                val responseValue = if (offset >= value.size) {
                    ByteArray(0)
                } else {
                    value.copyOfRange(offset, value.size)
                }

                gattServer?.sendResponse(
                    device, requestId,
                    android.bluetooth.BluetoothGatt.GATT_SUCCESS,
                    offset, responseValue
                )

                // Track that this device read this characteristic
                val charName = when (characteristic.uuid) {
                    CHARACTERISTIC_SSID -> "ssid"
                    CHARACTERISTIC_PASSWORD -> "password"
                    CHARACTERISTIC_SERVER_PORT -> "serverPort"
                    else -> "unknown"
                }
                deviceReadProgress[device.address]?.add(charName)
                Log.d(TAG, "Dezibot ${device.address} read $charName")
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException in onCharacteristicReadRequest: ${e.message}")
            }
        }
    }

    private fun stopGattServer() {
        try {
            gattServer?.close()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException closing GATT server: ${e.message}")
        }
        gattServer = null
    }

    // ==================== BLE Advertising ====================

    private fun startBleAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0) // Advertise indefinitely
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Save space for service UUID
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            Log.d(TAG, "BLE advertising started with service UUID: $SERVICE_UUID")
        } catch (e: SecurityException) {
            listener?.onBleError("BLE advertise permission denied: ${e.message}")
        } catch (e: Exception) {
            listener?.onBleError("Failed to start BLE advertising: ${e.message}")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            Log.d(TAG, "BLE advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            val reason = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error ($errorCode)"
            }
            Log.e(TAG, "BLE advertising failed: $reason")
            listener?.onBleError("BLE advertising failed: $reason")
        }
    }

    private fun stopBleAdvertising() {
        if (isAdvertising) {
            try {
                advertiser?.stopAdvertising(advertiseCallback)
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException stopping advertising: ${e.message}")
            }
            isAdvertising = false
        }
        advertiser = null
    }

    companion object {
        private const val TAG = "DezibotBleSharer"

        /**
         * Service UUID for the Dezibot credential sharing GATT service.
         *
         * Dezibot firmware should scan for this UUID to discover the Android host.
         */
        val SERVICE_UUID: java.util.UUID =
            java.util.UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")

        /**
         * Characteristic UUID for the hotspot SSID.
         * Type: UTF-8 string. Read-only.
         */
        val CHARACTERISTIC_SSID: java.util.UUID =
            java.util.UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")

        /**
         * Characteristic UUID for the hotspot password.
         * Type: UTF-8 string. Read-only.
         */
        val CHARACTERISTIC_PASSWORD: java.util.UUID =
            java.util.UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")


        /**
         * Characteristic UUID for the WebSocket server port.
         * Type: UTF-8 string of integer (e.g. "8765"). Read-only.
         */
        val CHARACTERISTIC_SERVER_PORT: java.util.UUID =
            java.util.UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb")
    }
}
