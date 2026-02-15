package de.htwk.dezibot.data.requests.light

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.requests.Action
import de.htwk.dezibot.data.requests.DezibotRequest
import de.htwk.dezibot.data.requests.Target

/**
 * Available light sensors on the Dezibot.
 */
enum class LightSensor(val sensorName: String) {
    @Json(name = "IR_LEFT")
    IR_LEFT("IR_LEFT"),

    @Json(name = "IR_RIGHT")
    IR_RIGHT("IR_RIGHT"),

    @Json(name = "IR_FRONT")
    IR_FRONT("IR_FRONT"),

    @Json(name = "IR_BACK")
    IR_BACK("IR_BACK"),

    @Json(name = "DL_FRONT")
    DL_FRONT("DL_FRONT"),

    @Json(name = "DL_BOTTOM")
    DL_BOTTOM("DL_BOTTOM")
}

/**
 * Light sensor types.
 */
enum class LightSensorType(val typeName: String) {
    @Json(name = "IR")
    IR("IR"),

    @Json(name = "DAYLIGHT")
    DAYLIGHT("DAYLIGHT")
}

/**
 * Request to get the value of a specific light sensor.
 *
 * @param sensor Name of the sensor to read
 */
@JsonClass(generateAdapter = true)
data class GetValueRequest(
    override val action: Action = Action.GET,
    override val target: Target = Target.LIGHT,
    override val command: String = "getValue",
    val sensor: String
) : DezibotRequest {
    companion object {
        fun create(sensor: LightSensor) = GetValueRequest(sensor = sensor.sensorName)
    }
}

/**
 * Request to get the brightest sensor of a specific type.
 *
 * @param type Sensor type (IR or DAYLIGHT)
 */
@JsonClass(generateAdapter = true)
data class GetBrightestRequest(
    override val action: Action = Action.GET,
    override val target: Target = Target.LIGHT,
    override val command: String = "getBrightest",
    val type: String
) : DezibotRequest {
    companion object {
        fun create(type: LightSensorType) = GetBrightestRequest(type = type.typeName)
    }
}

/**
 * Request to get the average value of a sensor over multiple measurements.
 *
 * @param sensor Name of the sensor
 * @param measurements Number of measurements (default: 1)
 * @param timeBetween Time between measurements in ms (default: 0)
 */
@JsonClass(generateAdapter = true)
data class GetAverageValueRequest(
    override val action: Action = Action.GET,
    override val target: Target = Target.LIGHT,
    override val command: String = "getAverageValue",
    val sensor: String,
    val measurements: Int = 1,
    val timeBetween: Int = 0
) : DezibotRequest {
    companion object {
        fun create(
            sensor: LightSensor,
            measurements: Int = 1,
            timeBetween: Int = 0
        ) = GetAverageValueRequest(
            sensor = sensor.sensorName,
            measurements = measurements,
            timeBetween = timeBetween
        )
    }
}
