package de.htwk.dezibot.data.responses.light

import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.responses.DezibotResponse
import de.htwk.dezibot.data.responses.Status

/**
 * Response for getValue command.
 *
 * @param value The sensor value
 */
@JsonClass(generateAdapter = true)
data class GetValueResponse(
    override val status: Status,
    override val target: String? = "light",
    override val command: String? = "getValue",
    val sensor: String? = null,
    val value: Int? = null
) : DezibotResponse

/**
 * Response for getBrightest command.
 *
 * @param sensor Name of the brightest sensor
 */
@JsonClass(generateAdapter = true)
data class GetBrightestResponse(
    override val status: Status,
    override val target: String? = "light",
    override val command: String? = "getBrightest",
    val type: String? = null,
    val sensor: String? = null
) : DezibotResponse

/**
 * Response for getAverageValue command.
 *
 * @param average The average sensor value
 */
@JsonClass(generateAdapter = true)
data class GetAverageValueResponse(
    override val status: Status,
    override val target: String? = "light",
    override val command: String? = "getAverageValue",
    val sensor: String? = null,
    val average: Int? = null,
    val measurements: Int? = null,
    val timeBetween: Int? = null
) : DezibotResponse
