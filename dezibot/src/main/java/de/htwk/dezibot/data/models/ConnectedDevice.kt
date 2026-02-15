package de.htwk.dezibot.data.models

/**
 * Represents a connected Dezibot device.
 *
 * @property ip The IP address of the device.
 * @property displayName The randomly generated name of the Dezibot, set via the showName command.
 */
data class ConnectedDevice(
    val ip: String,
    val clientId: String,
    val displayName: String? = null,
    val isActive: Boolean = true,
)
