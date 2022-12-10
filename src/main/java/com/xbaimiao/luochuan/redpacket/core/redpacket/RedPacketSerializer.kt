package com.xbaimiao.luochuan.redpacket.core.redpacket

interface RedPacketSerializer {

    fun serialize(redPacket: RedPacket): String?

    fun deserialize(string: String): RedPacket?

}