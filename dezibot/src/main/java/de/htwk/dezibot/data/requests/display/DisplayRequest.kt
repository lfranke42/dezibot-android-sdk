package de.htwk.dezibot.data.requests.display

import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.requests.Action
import de.htwk.dezibot.data.requests.DezibotRequest
import de.htwk.dezibot.data.requests.Target

/**
 * Request to print text on the display without a line break.
 *
 * @param text The text to display
 */
@JsonClass(generateAdapter = true)
data class PrintRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.DISPLAY,
    override val command: String = "print",
    val text: String
) : DezibotRequest

/**
 * Request to print text on the display with a line break.
 *
 * @param text The text to display
 */
@JsonClass(generateAdapter = true)
data class PrintlnRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.DISPLAY,
    override val command: String = "println",
    val text: String
) : DezibotRequest

/**
 * Request to clear all content from the display.
 */
@JsonClass(generateAdapter = true)
data class ClearRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.DISPLAY,
    override val command: String = "clear"
) : DezibotRequest

/**
 * Request to flip the display orientation by 180°.
 * Calling this command again resets the orientation.
 */
@JsonClass(generateAdapter = true)
data class FlipOrientationRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.DISPLAY,
    override val command: String = "flipOrientation"
) : DezibotRequest

/**
 * Request to invert the display colors.
 * Calling this command again cancels the inversion.
 */
@JsonClass(generateAdapter = true)
data class InvertColorRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.DISPLAY,
    override val command: String = "invertColor"
) : DezibotRequest

/**
 * Request to display the Dezibot's randomly generated name on its screen.
 * The response includes the name in the `dezibot` field.
 */
@JsonClass(generateAdapter = true)
data class ShowNameRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.DISPLAY,
    override val command: String = "showName"
) : DezibotRequest
