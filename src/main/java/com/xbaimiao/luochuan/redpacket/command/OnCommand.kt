package com.xbaimiao.luochuan.redpacket.command

import com.xbaimiao.easylib.sendLang
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.CommonRedPacket
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
            return arrayListOf("get", "send")
        }
        if (args.size == 2) {
            return arrayListOf("<红包金额>")
        }
        if (args.size == 3) {
            return arrayListOf("<数量>")
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
                        synchronized(RedPacket.lock) {
                            packet.send(sender)
                            LuoChuanRedPacket.redisManager.createOrUpdate(packet)
                        }
                    }
                    return true
                }

                "SEND" -> {
                    if (sender !is Player) {
                        sender.sendLang("command.not-player")
                        return true
                    }
                    val money = args[1].toIntOrNull() ?: return true
                    val num = args[2].toIntOrNull() ?: return true

                    if (money < num) {
                        sender.sendLang("redpacket.money-less-than-num")
                        return true
                    }

                    if (!HookVault.hasMoney(sender, money.toDouble())) {
                        sender.sendLang("redpacket.send-no-money")
                        return true
                    }
                    HookVault.takeMoney(sender, money.toDouble())

                    val redPacket = CommonRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        money, num,
                        money, num,
                        sender.name
                    )

                    LuoChuanRedPacket.redisManager.createOrUpdate(redPacket)
                    RedPacketManager.addRedPacket(redPacket)

                    val component = redPacket.toComponent()
                        .clickEvent(ClickEvent.runCommand("/luochuanredpacket get ${redPacket.id}"))
                        .hoverEvent(HoverEvent.showText(Component.text("点击领取")))

                    val serializer = GsonComponentSerializer.gson().serialize(component)

                    LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_PACKET, serializer))
                    return true
                }
            }

        }
        return true
    }

}