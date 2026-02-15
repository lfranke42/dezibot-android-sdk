package de.htwk.dezibot.data.requests.led

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import de.htwk.dezibot.data.requests.Action
import de.htwk.dezibot.data.requests.DezibotRequest
import de.htwk.dezibot.data.requests.Target

/**
 * LED positions on the Dezibot.
 * - TOP_LEFT: Left LED when robot faces away from you
 * - TOP_RIGHT: Right LED when robot faces away from you
 * - BOTTOM: Bottom LED
 * - TOP: Both top LEDs
 * - ALL: All three LEDs
 */
enum class LedPosition(val positionName: String) {
    @Json(name = "TOP_LEFT")
    TOP_LEFT("TOP_LEFT"),

    @Json(name = "TOP_RIGHT")
    TOP_RIGHT("TOP_RIGHT"),

    @Json(name = "BOTTOM")
    BOTTOM("BOTTOM"),

    @Json(name = "TOP")
    TOP("TOP"),

    @Json(name = "ALL")
    ALL("ALL")
}

/**
 * Request to set an LED by its index using a hex color value.
 * LED indexes: 0 = Right, 1 = Left, 2 = Bottom
 *
 * @param index LED index (0-2)
 * @param color Color hex value (0x00RRGGBB format)
 */
@JsonClass(generateAdapter = true)
data class SetLedByIndexColorRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "setLed",
    val index: Int,
    val color: Long
) : DezibotRequest

/**
 * Request to set an LED by its index using RGB values.
 * LED indexes: 0 = Right, 1 = Left, 2 = Bottom
 *
 * @param index LED index (0-2)
 * @param red Red value (0-100)
 * @param green Green value (0-100)
 * @param blue Blue value (0-100)
 */
@JsonClass(generateAdapter = true)
data class SetLedByIndexRgbRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "setLed",
    val index: Int,
    val red: Int,
    val green: Int,
    val blue: Int
) : DezibotRequest

/**
 * Request to set LED(s) by position using a hex color value.
 *
 * @param position LED position
 * @param color Color hex value (0x00RRGGBB format)
 */
@JsonClass(generateAdapter = true)
data class SetLedByPositionColorRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "setLed",
    val position: String,
    val color: Long
) : DezibotRequest {
    companion object {
        fun create(position: LedPosition, color: Long) =
            SetLedByPositionColorRequest(position = position.positionName, color = color)
    }
}

/**
 * Request to set LED(s) by position using RGB values.
 *
 * @param position LED position
 * @param red Red value (0-100)
 * @param green Green value (0-100)
 * @param blue Blue value (0-100)
 */
@JsonClass(generateAdapter = true)
data class SetLedByPositionRgbRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "setLed",
    val position: String,
    val red: Int,
    val green: Int,
    val blue: Int
) : DezibotRequest {
    companion object {
        fun create(position: LedPosition, red: Int, green: Int, blue: Int) =
            SetLedByPositionRgbRequest(
                position = position.positionName,
                red = red,
                green = green,
                blue = blue
            )
    }
}

/**
 * Request to set both top LEDs using a hex color value.
 *
 * @param color Color hex value (0x00RRGGBB format)
 */
@JsonClass(generateAdapter = true)
data class SetTopLedsColorRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "setTopLeds",
    val color: Long
) : DezibotRequest

/**
 * Request to set both top LEDs using RGB values.
 *
 * @param red Red value (0-100)
 * @param green Green value (0-100)
 * @param blue Blue value (0-100)
 */
@JsonClass(generateAdapter = true)
data class SetTopLedsRgbRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "setTopLeds",
    val red: Int,
    val green: Int,
    val blue: Int
) : DezibotRequest

/**
 * Request to blink LEDs by indexes.
 *
 * @param amount Number of times to blink
 * @param indexes Array of LED indexes (0: Right, 1: Left, 2: Bottom)
 * @param color Optional color hex value (default: blue)
 * @param interval Milliseconds the LED is on per blink (default: 50)
 */
@JsonClass(generateAdapter = true)
data class BlinkByIndexesRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "blink",
    val amount: Int,
    val indexes: List<Int>,
    val color: Long? = null,
    val interval: Int? = null
) : DezibotRequest

/**
 * Request to blink LEDs by indexes using RGB values.
 *
 * @param amount Number of times to blink
 * @param indexes Array of LED indexes
 * @param red Red value (0-100)
 * @param green Green value (0-100)
 * @param blue Blue value (0-100)
 * @param interval Milliseconds the LED is on per blink (default: 50)
 */
@JsonClass(generateAdapter = true)
data class BlinkByIndexesRgbRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "blink",
    val amount: Int,
    val indexes: List<Int>,
    val red: Int,
    val green: Int,
    val blue: Int,
    val interval: Int? = null
) : DezibotRequest

/**
 * Request to blink LEDs by position.
 *
 * @param amount Number of times to blink
 * @param position LED position to blink
 * @param color Optional color hex value (default: blue)
 * @param interval Milliseconds the LED is on per blink (default: 50)
 */
@JsonClass(generateAdapter = true)
data class BlinkByPositionRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "blink",
    val amount: Int,
    val position: String,
    val color: Long? = null,
    val interval: Int? = null
) : DezibotRequest {
    companion object {
        fun create(
            amount: Int,
            position: LedPosition,
            color: Long? = null,
            interval: Int? = null
        ) = BlinkByPositionRequest(
            amount = amount,
            position = position.positionName,
            color = color,
            interval = interval
        )
    }
}

/**
 * Request to blink LEDs by position using RGB values.
 *
 * @param amount Number of times to blink
 * @param position LED position
 * @param red Red value (0-100)
 * @param green Green value (0-100)
 * @param blue Blue value (0-100)
 * @param interval Milliseconds the LED is on per blink (default: 50)
 */
@JsonClass(generateAdapter = true)
data class BlinkByPositionRgbRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "blink",
    val amount: Int,
    val position: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val interval: Int? = null
) : DezibotRequest {
    companion object {
        fun create(
            amount: Int,
            position: LedPosition,
            red: Int,
            green: Int,
            blue: Int,
            interval: Int? = null
        ) = BlinkByPositionRgbRequest(
            amount = amount,
            position = position.positionName,
            red = red,
            green = green,
            blue = blue,
            interval = interval
        )
    }
}

/**
 * Request to turn off LED(s).
 *
 * @param position Optional LED position to turn off (default: ALL)
 */
@JsonClass(generateAdapter = true)
data class TurnOffLedsRequest(
    override val action: Action = Action.SET,
    override val target: Target = Target.MULTI_COLOR_LIGHT,
    override val command: String = "turnOff",
    val position: String? = null
) : DezibotRequest {
    companion object {
        fun create(position: LedPosition? = null) =
            TurnOffLedsRequest(position = position?.positionName)
    }
}
