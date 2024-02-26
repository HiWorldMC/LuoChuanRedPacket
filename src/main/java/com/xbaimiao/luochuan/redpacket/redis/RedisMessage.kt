package com.xbaimiao.luochuan.redpacket.redis

import com.google.gson.Gson
import com.google.gson.JsonObject

data class RedisMessage(
    val type: String, val message: String
) {

    fun serialize(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun deserialize(string: String): RedisMessage {
            return Gson().fromJson(string, RedisMessage::class.java)
        }

        const val TYPE_PACKET = "REDPACKET"
        const val TYPE_SEND_MESSAGE = "SEND_MESSAGE"
        const val TYPE_SEND_TOAST = "SEND_TOAST"
        const val TYPE_BC = "BC"

        fun typePacket(message: String, canGet: Boolean, id: String = ""): RedisMessage {
            return RedisMessage(TYPE_PACKET, JsonObject().apply {
                addProperty("message", message)
                addProperty("canGet", canGet)
                addProperty("id", id)
            }.toString())
        }

    }

}
