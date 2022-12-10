package com.xbaimiao.luochuan.redpacket.redis.message

import com.google.gson.Gson

class PlayerMessage(
    val player: String, val message: String
) {

    fun serialize(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun deserialize(string: String): PlayerMessage {
            return Gson().fromJson(string, PlayerMessage::class.java)
        }

    }

}