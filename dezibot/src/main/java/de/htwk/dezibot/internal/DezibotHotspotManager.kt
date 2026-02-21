package de.htwk.dezibot.internal

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Manages the local-only WiFi hotspot that ESP32 devices connect to.
 * Uses Android's LocalOnlyHotspot API (Android 8.0+).
 *
 * Note: Android generates random SSID/password for LocalOnlyHotspot for security.
 * Custom credentials are not supported for regular (non-system) apps.
 */
internal class DezibotHotspotManager(private val context: Context) {

    interface Listener {
        fun onHotspotStarted(ssid: String, password: String)
        fun onHotspotStopped()
        fun onHotspotFailed(reason: String)
    }

    var listener: Listener? = null

    /** The current hotspot SSID, available after onHotspotStarted is called. */
    var currentSsid: String? = null
        private set

    /** The current hotspot password, available after onHotspotStarted is called. */
    var currentPassword: String? = null
        private set

    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    /**
     * Start a local-only hotspot.
     * Requires CHANGE_WIFI_STATE permission and location permission at runtime.
     *
     * Note: Android generates random SSID and password each time for security.
     * The credentials are provided via the onHotspotStarted callback.
     *
     * ESP32 devices should connect to this hotspot and then connect to the
     * WebSocket server at 192.168.43.1:${port}.
     */
    fun startHotspot() {
        val handler = Handler(Looper.getMainLooper())

        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    hotspotReservation = reservation

                    // Get the actual SSID and password from the reservation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val softApConfig = reservation.softApConfiguration
                        val ssid = softApConfig.ssid ?: "Unknown"
                        val password = softApConfig.passphrase ?: "Unknown"
                        currentSsid = ssid
                        currentPassword = password
                        listener?.onHotspotStarted(ssid, password)
                    } else {
                        val config = reservation.wifiConfiguration
                        val ssid = config?.SSID ?: "Unknown"
                        val password = config?.preSharedKey ?: "Unknown"
                        currentSsid = ssid
                        currentPassword = password
                        listener?.onHotspotStarted(ssid, password)
                    }

                }

                override fun onStopped() {
                    hotspotReservation = null
                    currentSsid = null
                    currentPassword = null
                    listener?.onHotspotStopped()
                }

                override fun onFailed(reason: Int) {
                    val reasonStr = when (reason) {
                        ERROR_NO_CHANNEL -> "No channel available"
                        ERROR_GENERIC -> "Generic error"
                        ERROR_INCOMPATIBLE_MODE -> "Incompatible mode"
                        ERROR_TETHERING_DISALLOWED -> "Tethering disallowed"
                        else -> "Unknown error ($reason)"
                    }
                    listener?.onHotspotFailed(reasonStr)
                }
            }, handler)
        } catch (e: SecurityException) {
            listener?.onHotspotFailed("Permission denied: ${e.message}")
        } catch (e: Exception) {
            listener?.onHotspotFailed("Failed to start hotspot: ${e.message}")
        }
    }

    /**
     * Stop the local-only hotspot.
     */
    fun stopHotspot() {
        hotspotReservation?.close()
        hotspotReservation = null
    }

    /**
     * Check if the hotspot is currently active.
     */
    fun isHotspotActive(): Boolean = hotspotReservation != null
}
