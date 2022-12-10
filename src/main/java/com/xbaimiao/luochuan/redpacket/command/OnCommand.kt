package com.xbaimiao.luochuan.redpacket.command

import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.CommonRedPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import java.util.*

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
        if (p3.isNotEmpty()) {
            val id = p3[0]
            p0.sendMessage(LuoChuanRedPacket.redisManager.getRedPacket(id).toString())
            return true
        }

        val redPacket = CommonRedPacket(
            UUID.randomUUID().toString().replace("-", ""),
            100.0, 10,
            100.0, 10,
            "LuoChuan"
        )

        LuoChuanRedPacket.redisManager.createOrUpdate(redPacket)

        val serializer = GsonComponentSerializer.gson()
            .serialize(
                Component.text("测试消息(测试)").clickEvent(ClickEvent.runCommand("/luochuanredpacket ${redPacket.id}"))
            )

        LuoChuanRedPacket.redisManager.push(serializer)
        return true
    }

}