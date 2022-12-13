package com.xbaimiao.luochuan.redpacket.core.redpacket

import com.google.gson.annotations.SerializedName
import com.xbaimiao.easylib.info
import com.xbaimiao.easylib.sendLang
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.serializer.RedPacketSerializerGson
import com.xbaimiao.luochuan.redpacket.redis.RedisMessage
import com.xbaimiao.luochuan.redpacket.redis.message.PlayerMessage
import com.xbaimiao.luochuan.redpacket.serialize
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import top.mcplugin.lib.module.PlayerPoints.HookPlayerPoints
import top.mcplugin.lib.module.lang.Lang
import kotlin.reflect.KClass

data class PointsRedPacket(
    override val id: String,
    override val totalMoney: Int,
    override val totalNum: Int,
    override var remainMoney: Int,
    override var remainNum: Int,
    override val sender: String,
    private val receiveList: ArrayList<String> = arrayListOf()
) : AbstractRedPacket() {

    @SerializedName("pack")
    val divideRedPackage = initPackNum(totalMoney, totalNum)

    @SerializedName("sendList")
    val sendList = HashMap<String, Int>()

    @Synchronized
    override fun send(player: Player) {

        if (!cache.containsKey(id)) {
            cache[id] = java.util.ArrayList()
        }
        if (cache[id]!!.contains(player.name) || receiveList.contains(player.name)) {
            player.sendLang("redpacket.already-receive")
            return
        }

        if (remainNum <= 0 || remainMoney <= 0 || divideRedPackage.isEmpty()) {
            player.sendLang("redpacket.no-money")
            return
        }

        val money = divideRedPackage.removeAt(0)

        remainMoney -= money
        remainNum--

        HookPlayerPoints.addPoints(player, money)
        sendList[player.name] = money

        if (remainNum <= 0) {
            val max = sendList.maxBy { it.value }
            LuoChuanRedPacket.redisManager.push(
                RedisMessage(
                    RedisMessage.TYPE_PACKET,
                    Component.text(Lang.asLang<String>("redpacket.luck-king-points", sender, max.key, max.value))
                        .serialize()
                )
            )
        }

        player.sendLang("redpacket.receive-points", money)
        LuoChuanRedPacket.redisManager.push(
            RedisMessage(
                RedisMessage.TYPE_SEND_MESSAGE,
                PlayerMessage(
                    sender,
                    Lang.asLang("redpacket.player-receive-reply-points", player.name, money, remainMoney, remainNum)
                ).serialize()
            )
        )
        info("玩家 ${player.name} 领取了点券红包 ${toString()} 金额为 $money")
        receiveList.add(player.name)
        cache[id]!!.add(player.name)
    }

    override fun toComponent(): Component {
        return Component.text(Lang.asLang<String>("redpacket.send-points", sender, totalMoney))
    }

    companion object : RedPacketSerializerGson() {

        private val cache = hashMapOf<String, ArrayList<String>>()
        override fun getKey(): String = "PointsRedPacket"

        override fun getClass(): KClass<out RedPacket> = PointsRedPacket::class

    }

}