package com.xbaimiao.luochuan.redpacket.redis

import com.xbaimiao.luochuan.redpacket.redis.message.PlayerMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import redis.clients.jedis.JedisPubSub

class OnRedisMessage : JedisPubSub() {

    override fun onMessage(channel: String, message: String) {
        val redisMessage = RedisMessage.deserialize(message)
        when (redisMessage.type) {
            RedisMessage.TYPE_PACKET -> {
                val component = GsonComponentSerializer.gson().deserialize(redisMessage.message)
                for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(component)
                }
            }

            RedisMessage.TYPE_SEND_MESSAGE -> {
                val data = PlayerMessage.deserialize(redisMessage.message)
                val player = Bukkit.getPlayerExact(data.player) ?: return
                player.sendMessage(data.message)
            }
        }
    }

}