package de.htwk.dezibot.data.requests.motion

import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.requests.Action
import de.htwk.dezibot.data.requests.DezibotRequest
import de.htwk.dezibot.data.requests.Target

/**
 * Request to move the Dezibot forward with correction algorithm.
 *
 * @param durationMs Duration in milliseconds, 0 = continuous until stop
 * @param baseValue PWM base duty used to drive the motors (default: 3900)
 */
@JsonClass(generateAdapter = true)
data class MoveRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MOTION,
    override val command: String = "move",
    val durationMs: Long = 0,
    val baseValue: Int = 3900
) : DezibotRequest

/**
 * Request to rotate the Dezibot clockwise.
 *
 * @param durationMs Duration in milliseconds, 0 = continuous
 * @param baseValue PWM base duty used to drive the motors (default: 3900)
 */
@JsonClass(generateAdapter = true)
data class RotateClockwiseRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MOTION,
    override val command: String = "rotateClockwise",
    val durationMs: Long = 0,
    val baseValue: Int = 3900
) : DezibotRequest

/**
 * Request to rotate the Dezibot counter-clockwise.
 *
 * @param durationMs Duration in milliseconds, 0 = continuous
 * @param baseValue PWM base duty used to drive the motors (default: 3900)
 */
@JsonClass(generateAdapter = true)
data class RotateAntiClockwiseRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MOTION,
    override val command: String = "rotateAntiClockwise",
    val durationMs: Long = 0,
    val baseValue: Int = 3900
) : DezibotRequest

/**
 * Request to stop all Dezibot movement immediately.
 */
@JsonClass(generateAdapter = true)
data class StopRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MOTION,
    override val command: String = "stop"
) : DezibotRequest
