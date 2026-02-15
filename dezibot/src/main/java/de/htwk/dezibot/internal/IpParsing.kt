package de.htwk.dezibot.internal

/**
 * Parses a WebSocket client ID (e.g. "/192.168.4.2:54321" or "hostname/192.168.4.2:12345")
 * into a pair of (ip, uniqueId).
 *
 * - **ip**: The IP address portion (e.g. "192.168.4.2"), or empty string if unparseable.
 * - **uniqueId**: The client ID with the ephemeral port stripped (e.g. "/192.168.4.2"),
 *   or the original clientId if unparseable.
 *
 * @param clientId The raw client identifier from [org.java_websocket.WebSocket.getRemoteSocketAddress].
 * @return A [Pair] of (ip, uniqueId).
 */
internal fun parseClientId(clientId: String): Pair<String, String> {
    val ipStart = clientId.indexOf('/') + 1
    val portStart = clientId.lastIndexOf(':')

    if (ipStart <= 0 || portStart == -1 || portStart < ipStart) {
        return Pair("", clientId)
    }

    val ip = clientId.substring(ipStart, portStart)
    val uniqueId = clientId.take(portStart)
    return Pair(ip, uniqueId)
}
