package de.htwk.dezibot.data.requests

import com.squareup.moshi.Json

/**
 * Represents the action type for a Dezibot command.
 */
enum class Action {
    @Json(name = "set")
    SET,

    @Json(name = "get")
    GET
}

/**
 * Represents the target component of the Dezibot.
 */
enum class Target {
    @Json(name = "motion")
    MOTION,

    @Json(name = "display")
    DISPLAY,

    @Json(name = "color")
    COLOR,

    @Json(name = "light")
    LIGHT,

    @Json(name = "multiColorLight")
    MULTI_COLOR_LIGHT
}

/**
 * Base interface for all Dezibot requests.
 * All requests share common fields: action, target, and command.
 */
interface DezibotRequest {
    val action: Action
    val target: Target
    val command: String
}
