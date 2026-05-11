package com.example.tictactoeapplication.multiplayer

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class MessageType {
    START,
    MOVE,
    RESET
}

@Serializable
data class GameMove(
    val index: Int = -1,
    val player: String = "",
    val type: MessageType = MessageType.MOVE
) {
    fun toByteArray(): ByteArray = Json.encodeToString(this).toByteArray(Charsets.UTF_8)

    companion object {
        fun fromByteArray(data: ByteArray): GameMove =
            Json.decodeFromString(String(data, Charsets.UTF_8))
    }
}
