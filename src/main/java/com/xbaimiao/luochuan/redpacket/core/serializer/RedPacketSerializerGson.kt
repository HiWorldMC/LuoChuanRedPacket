package com.xbaimiao.luochuan.redpacket.core.serializer

import com.google.gson.Gson
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import kotlin.reflect.KClass

abstract class RedPacketSerializerGson : RedPacketSerializer {

    abstract fun getKey(): String
    abstract fun getClass(): KClass<out RedPacket>

    override fun serialize(redPacket: RedPacket): String? {
        if (redPacket::class != getClass()) {
            return null
        }
        return getKey() + Gson().toJson(redPacket)
    }

    override fun deserialize(string: String): RedPacket? {
        if (!string.startsWith(getKey())) {
            return null
        }
        return Gson().fromJson(string.substring(getKey().length), getClass().java)
    }


}