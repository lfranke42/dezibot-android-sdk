package de.htwk.dezibot.data.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Status of a Dezibot response.
 */
enum class Status {
    @Json(name = "ok")
    OK,

    @Json(name = "error")
    ERROR
}

/**
 * Base interface for all Dezibot responses.
 */
interface DezibotResponse {
    val status: Status
    val target: String?
    val command: String?
}

/**
 * Generic error response from the Dezibot.
 */
@JsonClass(generateAdapter = true)
data class ErrorResponse(
    override val status: Status = Status.ERROR,
    override val target: String? = null,
    override val command: String? = null,
    val message: String
) : DezibotResponse
