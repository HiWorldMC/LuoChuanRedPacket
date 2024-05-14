package com.xbaimiao.luochuan.redpacket.core.listener

import com.xbaimiao.easylib.util.submit
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class OnChat : Listener {

    @EventHandler
    fun chat(event: AsyncPlayerChatEvent) {
        val message = event.message
        for (textRedPacket in LuoChuanRedPacket.redisManager.getTextRedPackets()) {
            if (textRedPacket.second == message) {
                val id = textRedPacket.first.split(":")[1]
                val player = event.player
                //LuoChuanRedPacket:890a48f412d0499db17d396192482cc6:text
                submit {
                    if (!LuoChuanRedPacket.redisManager.canGetRedPacket(player, id)) {
                        return@submit
                    }
                    LuoChuanRedPacket.redisManager.getRedPacket(player, id) {
                        if (this != null) {
                            this.send(player)
                            LuoChuanRedPacket.redisManager.createOrUpdate(this)
                        }
                    }
                }
            }
        }
    }

}
