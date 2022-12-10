package com.xbaimiao.luochuan.redpacket.redis

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import redis.clients.jedis.JedisPubSub

class OnRedisMessage : JedisPubSub() {

    override fun onMessage(channel: String, message: String) {
        val component = GsonComponentSerializer.gson().deserialize(message)
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(component)
        }
    }

}