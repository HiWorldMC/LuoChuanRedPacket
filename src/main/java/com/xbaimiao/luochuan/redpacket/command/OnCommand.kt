package com.xbaimiao.luochuan.redpacket.command

import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class OnCommand : TabExecutor {
    override fun onTabComplete(
        p0: CommandSender,
        p1: Command,
        p2: String,
        p3: Array<out String>
    ): MutableList<String>? {
        return null
    }

    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): Boolean {

        val serializer = GsonComponentSerializer.gson()
            .serialize(
                Component.text("测试消息(测试)").clickEvent(ClickEvent.openUrl("https://jd.adventure.kyori.net/"))
            )

        LuoChuanRedPacket.redisManager.push(serializer)
        return true
    }

}