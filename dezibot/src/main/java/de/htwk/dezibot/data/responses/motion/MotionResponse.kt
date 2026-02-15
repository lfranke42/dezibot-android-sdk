package de.htwk.dezibot.data.responses.motion

import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.responses.DezibotResponse
import de.htwk.dezibot.data.responses.Status

/**
 * Response for move command.
 */
@JsonClass(generateAdapter = true)
data class MoveResponse(
    override val status: Status,
    override val target: String? = "motion",
    override val command: String? = "move",
    val durationMs: Long? = null,
    val baseValue: Int? = null
) : DezibotResponse

/**
 * Response for rotateClockwise command.
 */
@JsonClass(generateAdapter = true)
data class RotateClockwiseResponse(
    override val status: Status,
    override val target: String? = "motion",
    override val command: String? = "rotateClockwise",
    val durationMs: Long? = null,
    val baseValue: Int? = null
) : DezibotResponse

/**
 * Response for rotateAntiClockwise command.
 */
@JsonClass(generateAdapter = true)
data class RotateAntiClockwiseResponse(
    override val status: Status,
    override val target: String? = "motion",
    override val command: String? = "rotateAntiClockwise",
    val durationMs: Long? = null,
    val baseValue: Int? = null
) : DezibotResponse

/**
 * Response for stop command.
 */
@JsonClass(generateAdapter = true)
data class StopResponse(
    override val status: Status,
    override val target: String? = "motion",
    override val command: String? = "stop"
) : DezibotResponse
