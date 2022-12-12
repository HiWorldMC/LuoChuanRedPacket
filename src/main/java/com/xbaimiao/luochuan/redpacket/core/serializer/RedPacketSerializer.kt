package com.xbaimiao.luochuan.redpacket.core.serializer

import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket

interface RedPacketSerializer {

    fun serialize(redPacket: RedPacket): String?

    fun deserialize(string: String): RedPacket?

}