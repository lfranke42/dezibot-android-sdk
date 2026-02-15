package de.htwk.dezibot.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DezibotCommand(
    val type: String,
    val value: Int,
    val extra: String? = null
)