package de.htwk.dezibot.data.responses.color

import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.requests.color.ColorSensorMode
import de.htwk.dezibot.data.responses.DezibotResponse
import de.htwk.dezibot.data.responses.Status

/**
 * Response for configure command.
 */
@JsonClass(generateAdapter = true)
data class ConfigureColorResponse(
    override val status: Status,
    override val target: String? = "color",
    override val command: String? = "configure",
    val mode: ColorSensorMode? = null,
    val enabled: Boolean? = null,
    val exposureTime: Int? = null
) : DezibotResponse

/**
 * Response for beginAutoMode command.
 */
@JsonClass(generateAdapter = true)
data class BeginAutoModeResponse(
    override val status: Status,
    override val target: String? = "color",
    override val command: String? = "beginAutoMode",
    val started: Boolean? = null
) : DezibotResponse

/**
 * Response for getColorValue command.
 *
 * @param value The sensor value (0-65535 range)
 */
@JsonClass(generateAdapter = true)
data class GetColorValueResponse(
    override val status: Status,
    override val target: String? = "color",
    override val command: String? = "getColorValue",
    val color: String? = null,
    val value: Int? = null
) : DezibotResponse

/**
 * Response for getAmbientLight command.
 *
 * @param lux The ambient light level in lux
 */
@JsonClass(generateAdapter = true)
data class GetAmbientLightResponse(
    override val status: Status,
    override val target: String? = "color",
    override val command: String? = "getAmbientLight",
    val lux: Float? = null
) : DezibotResponse
