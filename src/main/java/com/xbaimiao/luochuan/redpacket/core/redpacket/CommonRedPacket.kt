package com.xbaimiao.luochuan.redpacket.core.redpacket

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.xbaimiao.easylib.info
import com.xbaimiao.easylib.sendLang
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.redis.RedisMessage
import com.xbaimiao.luochuan.redpacket.redis.message.PlayerMessage
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import top.mcplugin.lib.module.lang.Lang
import top.mcplugin.lib.module.vault.HookVault
import java.util.*


data class CommonRedPacket(
    override val id: String,
    override val totalMoney: Int,
    override val totalNum: Int,
    override var remainMoney: Int,
    override var remainNum: Int,
    override val sender: String,
    val receiveList: ArrayList<String> = arrayListOf()
) : RedPacket {

    @SerializedName("pack")
    val divideRedPackage = initPackNum(totalMoney, totalNum)

    override fun send(player: Player) {
        if (!cache.containsKey(id)) {
            cache[id] = ArrayList()
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

        HookVault.addMoney(player, money.toDouble())
        player.sendLang("redpacket.receive", money)
        LuoChuanRedPacket.redisManager.push(
            RedisMessage(
                RedisMessage.TYPE_SEND_MESSAGE,
                PlayerMessage(
                    sender,
                    Lang.asLang("redpacket.player-receive-reply", player.name, money, remainMoney, remainNum)
                ).serialize()
            )
        )
        info("玩家 ${player.name} 领取了红包 ${toString()} 金额为 $money")
        receiveList.add(player.name)
        cache[id]!!.add(player.name)
    }

    private fun initPackNum(totalAmount: Int, totalPeopleNum: Int): ArrayList<Int> {
        val amountList = ArrayList<Int>()
        var restAmount = totalAmount
        var restPeopleAmount = totalPeopleNum
        for (i in 0 until totalPeopleNum - 1) {
            val amount: Int = random.nextInt(restAmount / restPeopleAmount * 2 - 1) + 1
            restAmount -= amount
            restPeopleAmount--
            amountList.add(amount)
        }
        amountList.add(restAmount)
        return amountList
    }


    override fun toComponent(): Component {
        return Component.text(Lang.asLang<String>("redpacket.send-common", sender, totalMoney))
    }

    companion object : RedPacketSerializer {
        private val random = Random()

        private const val KEY = "CommonRedPacket"
        private val cache = hashMapOf<String, ArrayList<String>>()

        override fun serialize(redPacket: RedPacket): String? {
            if (redPacket !is CommonRedPacket) {
                return null
            }
            return KEY + Gson().toJson(redPacket)
        }

        override fun deserialize(string: String): RedPacket? {
            if (!string.startsWith(KEY)) {
                return null
            }
            return Gson().fromJson(string.substring(KEY.length), CommonRedPacket::class.java)
        }

    }

}