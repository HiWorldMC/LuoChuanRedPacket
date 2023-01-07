package com.xbaimiao.luochuan.redpacket.core.redpacket

import com.xbaimiao.luochuan.redpacket.core.serializer.RedPacketSerializer
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

interface RedPacket {

    // 红包ID 唯一且不重复
    val id: String

    // 总金额
    val totalMoney: Int

    // 总数量
    val totalNum: Int

    // 剩余金额
    val remainMoney: Int

    // 剩余数量
    val remainNum: Int

    // 发送者
    val sender: String

    fun send(player: Player)

    fun toComponent(): Component

    // 口令红包消息
    fun getTextRedPackMessage(text: String): String

    companion object {

        val lock = Any()

        private val serializers = arrayListOf<RedPacketSerializer>().also {
            it.add(CommonRedPacket)
            it.add(PointsRedPacket)
        }

        fun serialize(redPacket: RedPacket): String {
            for (serializer in serializers) {
                val string = serializer.serialize(redPacket)
                if (string != null) {
                    return string
                }
            }
            error("序列化失败")
        }

        fun deserialize(string: String): RedPacket? {
            for (serializer in serializers) {
                val redPacket = serializer.deserialize(string)
                if (redPacket != null) {
                    return redPacket
                }
            }
            return null
        }

    }


}