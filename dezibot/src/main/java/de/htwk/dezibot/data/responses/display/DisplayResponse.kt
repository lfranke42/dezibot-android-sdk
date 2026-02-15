package de.htwk.dezibot.data.responses.display

import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.responses.DezibotResponse
import de.htwk.dezibot.data.responses.Status

/**
 * Response for print command.
 */
@JsonClass(generateAdapter = true)
data class PrintResponse(
    override val status: Status,
    override val target: String? = "display",
    override val command: String? = "print",
    val text: String? = null
) : DezibotResponse

/**
 * Response for println command.
 */
@JsonClass(generateAdapter = true)
data class PrintlnResponse(
    override val status: Status,
    override val target: String? = "display",
    override val command: String? = "println",
    val text: String? = null
) : DezibotResponse

/**
 * Response for clear command.
 */
@JsonClass(generateAdapter = true)
data class ClearResponse(
    override val status: Status,
    override val target: String? = "display",
    override val command: String? = "clear"
) : DezibotResponse

/**
 * Response for flipOrientation command.
 */
@JsonClass(generateAdapter = true)
data class FlipOrientationResponse(
    override val status: Status,
    override val target: String? = "display",
    override val command: String? = "flipOrientation"
) : DezibotResponse

/**
 * Response for invertColor command.
 */
@JsonClass(generateAdapter = true)
data class InvertColorResponse(
    override val status: Status,
    override val target: String? = "display",
    override val command: String? = "invertColor"
) : DezibotResponse

/**
 * Response for showName command.
 * Contains the randomly generated Dezibot name.
 */
@JsonClass(generateAdapter = true)
data class ShowNameResponse(
    override val status: Status,
    override val target: String? = "display",
    override val command: String? = "showName",
    val dezibot: String
) : DezibotResponse
