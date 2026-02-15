package de.htwk.dezibot.data.responses.led

import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.responses.DezibotResponse
import de.htwk.dezibot.data.responses.Status

/**
 * Response for setLed command with index.
 */
@JsonClass(generateAdapter = true)
data class SetLedByIndexResponse(
    override val status: Status,
    override val target: String? = "multiColorLight",
    override val command: String? = "setLed",
    val index: Int? = null,
    val color: Long? = null,
    val red: Int? = null,
    val green: Int? = null,
    val blue: Int? = null
) : DezibotResponse

/**
 * Response for setLed command with position.
 */
@JsonClass(generateAdapter = true)
data class SetLedByPositionResponse(
    override val status: Status,
    override val target: String? = "multiColorLight",
    override val command: String? = "setLed",
    val position: String? = null,
    val color: Long? = null,
    val red: Int? = null,
    val green: Int? = null,
    val blue: Int? = null
) : DezibotResponse

/**
 * Response for setTopLeds command.
 */
@JsonClass(generateAdapter = true)
data class SetTopLedsResponse(
    override val status: Status,
    override val target: String? = "multiColorLight",
    override val command: String? = "setTopLeds",
    val color: Long? = null,
    val red: Int? = null,
    val green: Int? = null,
    val blue: Int? = null
) : DezibotResponse

/**
 * Response for blink command with indexes.
 */
@JsonClass(generateAdapter = true)
data class BlinkByIndexesResponse(
    override val status: Status,
    override val target: String? = "multiColorLight",
    override val command: String? = "blink",
    val amount: Int? = null,
    val indexes: List<Int>? = null,
    val color: Long? = null,
    val interval: Int? = null
) : DezibotResponse

/**
 * Response for blink command with position.
 */
@JsonClass(generateAdapter = true)
data class BlinkByPositionResponse(
    override val status: Status,
    override val target: String? = "multiColorLight",
    override val command: String? = "blink",
    val amount: Int? = null,
    val position: String? = null,
    val color: Long? = null,
    val interval: Int? = null
) : DezibotResponse

/**
 * Response for turnOff command.
 */
@JsonClass(generateAdapter = true)
data class TurnOffLedsResponse(
    override val status: Status,
    override val target: String? = "multiColorLight",
    override val command: String? = "turnOff",
    val position: String? = null
) : DezibotResponse
