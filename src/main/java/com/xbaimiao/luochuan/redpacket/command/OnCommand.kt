package com.xbaimiao.luochuan.redpacket.command

import com.xbaimiao.easylib.sendLang
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.CommonRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.PointsRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import com.xbaimiao.luochuan.redpacket.redis.RedisMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import top.mcplugin.lib.module.PlayerPoints.HookPlayerPoints
import top.mcplugin.lib.module.vault.HookVault
import java.util.*

class OnCommand : TabExecutor {
    override fun onTabComplete(
        p0: CommandSender,
        p1: Command,
        p2: String,
        args: Array<out String>
    ): MutableList<String>? {
        if (args.size == 1) {
            return arrayListOf("send-vault", "send-points")
        }
        if (args.size >= 2 && args[0].uppercase() == "SEND-VAULT" || args[0].uppercase() == "SEND-POINTS") {
            if (args.size == 2) {
                return arrayListOf("<红包金额>")
            }
            if (args.size == 3) {
                return arrayListOf("<数量>")
            }
        }
        return null
    }

    override fun onCommand(sender: CommandSender, p1: Command, p2: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            when (args[0].uppercase()) {
                "GET" -> {
                    if (args.size < 2 || sender !is Player) {
                        sender.sendLang("command.not-player")
                        return true
                    }
                    val id = args[1]
                    LuoChuanRedPacket.redisManager.getRedPacket(id).whenComplete { packet, t ->
                        t?.printStackTrace()
                        if (packet == null) {
                            return@whenComplete
                        }
                        try {
                            packet.send(sender)
                            LuoChuanRedPacket.redisManager.createOrUpdate(packet)
                        } catch (th: Throwable) {
                            th.printStackTrace()
                        }
                    }
                    return true
                }

                "SEND-VAULT" -> {
                    val data = check(sender, args) ?: return true

                    if (!HookVault.hasMoney(data.player, data.money.toDouble())) {
                        sender.sendLang("redpacket.send-no-money")
                        return true
                    }
                    HookVault.takeMoney(data.player, data.money.toDouble())

                    val redPacket = CommonRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money, data.num,
                        data.money, data.num,
                        sender.name
                    )
                    send(redPacket)
                    return true
                }

                "SEND-POINTS" -> {
                    val data = check(sender, args) ?: return true

                    if (!HookPlayerPoints.hasPoints(data.player, data.money)) {
                        sender.sendLang("redpacket.send-no-money-points")
                        return true
                    }
                    HookPlayerPoints.takePoints(data.player, data.money)

                    val redPacket = PointsRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money, data.num,
                        data.money, data.num,
                        sender.name
                    )
                    send(redPacket)
                    return true
                }
            }

        }
        return true
    }

    private fun send(redPacket: RedPacket) {
        LuoChuanRedPacket.redisManager.createOrUpdate(redPacket)
        RedPacketManager.addRedPacket(redPacket)

        val component = redPacket.toComponent()
            .clickEvent(ClickEvent.runCommand("/luochuanredpacket get ${redPacket.id}"))
            .hoverEvent(HoverEvent.showText(Component.text("点击领取")))

        val serializer = GsonComponentSerializer.gson().serialize(component)

        LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_PACKET, serializer))
    }

    private fun check(sender: CommandSender, args: Array<out String>): SendData? {
        if (sender !is Player) {
            sender.sendLang("command.not-player")
            return null
        }
        val money = args.getOrNull(1)?.toIntOrNull()
        val num = args.getOrNull(2)?.toIntOrNull()

        if (money == null || num == null) {
            sender.sendLang("command.not-number")
            return null
        }

        if (money < 1 || num < 1) {
            sender.sendLang("command.number-too-small")
            return null
        }

        if (money < num) {
            sender.sendLang("redpacket.money-less-than-num")
            return null
        }
        return SendData(sender, money, num)
    }

}

data class SendData(
    val player: Player,
    val money: Int,
    val num: Int
)