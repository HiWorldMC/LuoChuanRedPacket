package com.xbaimiao.luochuan.redpacket.command

import com.xbaimiao.easylib.sendLang
import com.xbaimiao.easylib.submit
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.CommonRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.PointsRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import com.xbaimiao.luochuan.redpacket.data.PlayerProfile
import com.xbaimiao.luochuan.redpacket.redis.RedisMessage
import com.xbaimiao.luochuan.redpacket.serialize
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import top.mcplugin.lib.module.PlayerPoints.HookPlayerPoints
import top.mcplugin.lib.module.vault.HookVault
import java.util.*

class OnCommand : TabExecutor {
    override fun onTabComplete(
        p0: CommandSender, p1: Command, p2: String, args: Array<out String>
    ): MutableList<String>? {
        if (args.size == 1) {
            return arrayListOf(
                "send-vault", "send-points", "toggle", "send-text-vault", "send-text-points"
            ).filter { it.startsWith(args[0]) }.toMutableList()
        }
        if (args.size >= 2 && args[0].lowercase().contains("send")) {
            if (args.size == 2) {
                return arrayListOf("<红包金额>")
            }
            if (args.size == 3) {
                return arrayListOf("<数量>")
            }
            if (args[0].lowercase().contains("text")) {
                return arrayListOf("<口令>")
            }
        }
        return null
    }

    override fun onCommand(sender: CommandSender, p1: Command, p2: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty()) {
            when (args[0].uppercase()) {
                "TOGGLE" -> {
                    if (sender is Player) {
                        PlayerProfile.read(sender).thenAcceptAsync { profile ->
                            profile.animation = !profile.animation
                            PlayerProfile.save(profile)
                            sender.sendLang("redpacket.toggle", if (profile.animation) "开启" else "关闭")
                        }
                    }
                }

                "GET" -> {
                    if (args.size < 2 || sender !is Player) {
                        sender.sendLang("command.not-player")
                        return true
                    }
                    val id = args[1]
                    LuoChuanRedPacket.redisManager.getRedPacket(id) {

                        try {
                            this?.let {
                                it.send(sender)
                                LuoChuanRedPacket.redisManager.createOrUpdate(it)
                            }
                        } catch (th: Throwable) {
                            th.printStackTrace()
                        }
                    }
                    return true
                }

                "SEND-TEXT-POINTS" -> {
                    if (!sender.hasPermission("luochuanredpacket.command.points.text")) {
                        sender.sendLang("command.no-permission-points")
                        return true
                    }
                    val data = check(sender, args) ?: return true

                    if (data.text == null) {
                        sender.sendLang("redpacket.send-text-value-null")
                        return true
                    }

                    if (!HookPlayerPoints.hasPoints(data.player, data.money)) {
                        sender.sendLang("redpacket.send-no-money-points")
                        return true
                    }
                    HookPlayerPoints.takePoints(data.player, data.money)

                    val redPacket = PointsRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money,
                        data.num,
                        data.money,
                        data.num,
                        sender.name
                    )
                    sendText(data.player, redPacket, data.text)
                    return true
                }

                "SEND-TEXT-VAULT" -> {
                    if (!sender.hasPermission("luochuanredpacket.command.vault.text")) {
                        sender.sendLang("command.no-permission-vault")
                        return true
                    }
                    val data = check(sender, args) ?: return true

                    if (data.text == null) {
                        sender.sendLang("redpacket.send-text-value-null")
                        return true
                    }

                    if (!HookVault.hasMoney(data.player, data.money.toDouble())) {
                        sender.sendLang("redpacket.send-no-money")
                        return true
                    }
                    HookVault.takeMoney(data.player, data.money.toDouble())

                    val redPacket = CommonRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money,
                        data.num,
                        data.money,
                        data.num,
                        sender.name
                    )
                    sendText(data.player, redPacket, data.text)
                    return true
                }

                "SEND-VAULT" -> {
                    if (!sender.hasPermission("luochuanredpacket.command.vault")) {
                        sender.sendLang("command.no-permission-vault")
                        return true
                    }
                    val data = check(sender, args) ?: return true

                    if (!HookVault.hasMoney(data.player, data.money.toDouble())) {
                        sender.sendLang("redpacket.send-no-money")
                        return true
                    }
                    HookVault.takeMoney(data.player, data.money.toDouble())

                    val redPacket = CommonRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money,
                        data.num,
                        data.money,
                        data.num,
                        sender.name
                    )
                    send(redPacket)
                    return true
                }

                "SEND-POINTS" -> {
                    if (!sender.hasPermission("luochuanredpacket.command.points")) {
                        sender.sendLang("command.no-permission-points")
                        return true
                    }
                    val data = check(sender, args) ?: return true

                    if (!HookPlayerPoints.hasPoints(data.player, data.money)) {
                        sender.sendLang("redpacket.send-no-money-points")
                        return true
                    }
                    HookPlayerPoints.takePoints(data.player, data.money)

                    val redPacket = PointsRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money,
                        data.num,
                        data.money,
                        data.num,
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
        submit(async = true) {
            LuoChuanRedPacket.redisManager.createOrUpdate(redPacket)
            RedPacketManager.addRedPacket(redPacket)

            val component =
                redPacket.toComponent().clickEvent(ClickEvent.runCommand("/luochuanredpacket get ${redPacket.id}"))
                    .hoverEvent(HoverEvent.showText(Component.text("点击领取")))

            LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_PACKET, component.serialize()))
            LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_SEND_TOAST, "server:redpacket"))
        }
    }

    private fun sendText(sender: CommandSender, redPacket: RedPacket, text: String) {
        submit(async = true) {
            if (!ConfigManager.words.any { it.canSend(text) }) {
                sender.sendLang("command.not-pass")
                return@submit
            }
            LuoChuanRedPacket.redisManager.createOrUpdate(redPacket)
            LuoChuanRedPacket.redisManager.addTextRedPacket(redPacket, text)
            RedPacketManager.addRedPacket(redPacket)

            LuoChuanRedPacket.redisManager.push(
                RedisMessage(
                    RedisMessage.TYPE_BC, redPacket.getTextRedPackMessage(text)
                )
            )
            LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_SEND_TOAST, "server:redpacket"))
        }
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
        val text = args.getOrNull(3)
        return SendData(sender, money, num, text)
    }

}

data class SendData(
    val player: Player, val money: Int, val num: Int, val text: String?
)