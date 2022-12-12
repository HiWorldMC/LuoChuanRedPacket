package com.xbaimiao.luochuan.redpacket.redis

import com.google.gson.Gson

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

    }

}