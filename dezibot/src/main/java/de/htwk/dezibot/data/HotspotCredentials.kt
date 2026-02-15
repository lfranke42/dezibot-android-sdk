package de.htwk.dezibot.data

import com.squareup.moshi.JsonClass

/**
 * Data class representing the hotspot credentials to share with an ESP32 device.
 * Sent as JSON over WebSocket so the ESP32 can connect to the Android hotspot.
 */
@JsonClass(generateAdapter = true)
data class HotspotCredentials(
    val type: String = "hotspot_credentials",
    val ssid: String,
    val password: String,
    val serverIp: String,
    val serverPort: Int
)
