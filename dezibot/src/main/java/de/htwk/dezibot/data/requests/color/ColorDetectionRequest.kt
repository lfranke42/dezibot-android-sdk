package de.htwk.dezibot.data.requests.color

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.requests.Action
import de.htwk.dezibot.data.requests.DezibotRequest
import de.htwk.dezibot.data.requests.Target

/**
 * Mode for the color sensor.
 */
enum class ColorSensorMode {
    @Json(name = "AUTO")
    AUTO,

    @Json(name = "MANUAL")
    MANUAL
}

/**
 * Color channels available on the VEML6040 sensor.
 */
enum class ColorChannel(val jsonName: String) {
    @Json(name = "VEML_RED")
    RED("VEML_RED"),

    @Json(name = "VEML_GREEN")
    GREEN("VEML_GREEN"),

    @Json(name = "VEML_BLUE")
    BLUE("VEML_BLUE"),

    @Json(name = "VEML_WHITE")
    WHITE("VEML_WHITE")
}

/**
 * Supported exposure times (integration times) in milliseconds.
 */
enum class ExposureTime(val ms: Int) {
    MS_40(40),
    MS_80(80),
    MS_160(160),
    MS_320(320),
    MS_640(640),
    MS_1280(1280)
}

/**
 * Request to configure the color sensor.
 *
 * @param mode Sensor mode (AUTO or MANUAL)
 * @param enabled Whether the sensor is enabled
 * @param exposureTime Integration time in milliseconds
 */
@JsonClass(generateAdapter = true)
data class ConfigureColorRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.COLOR,
    override val command: String = "configure",
    val mode: ColorSensorMode,
    val enabled: Boolean,
    val exposureTime: Int
) : DezibotRequest {
    companion object {
        fun create(
            mode: ColorSensorMode,
            enabled: Boolean,
            exposureTime: ExposureTime
        ) = ConfigureColorRequest(
            mode = mode,
            enabled = enabled,
            exposureTime = exposureTime.ms
        )
    }
}

/**
 * Request to start the color sensor in automatic mode.
 */
@JsonClass(generateAdapter = true)
data class BeginAutoModeRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.COLOR,
    override val command: String = "beginAutoMode"
) : DezibotRequest

/**
 * Request to get a specific color channel value.
 *
 * @param color The color channel to read
 */
@JsonClass(generateAdapter = true)
data class GetColorValueRequest(
    override val action: Action = Action.GET,
    override val target: Target = Target.COLOR,
    override val command: String = "getColorValue",
    val color: String
) : DezibotRequest {
    companion object {
        fun create(channel: ColorChannel) = GetColorValueRequest(color = channel.jsonName)
    }
}

/**
 * Request to get the ambient light level in lux.
 */
@JsonClass(generateAdapter = true)
data class GetAmbientLightRequest(
    override val action: Action = Action.GET,
    override val target: Target = Target.COLOR,
    override val command: String = "getAmbientLight"
) : DezibotRequest
