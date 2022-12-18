package com.xbaimiao.luochuan.redpacket.redis

import com.xbaimiao.easylib.submit
import com.xbaimiao.luochuan.redpacket.data.PlayerProfile
import com.xbaimiao.luochuan.redpacket.redis.message.PlayerMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import redis.clients.jedis.JedisPubSub

class OnRedisMessage : JedisPubSub() {

    private var isSubscribed = true

    override fun onMessage(channel: String, message: String) {
        if (!isSubscribed) {
            return
        }
        val redisMessage = try {
            RedisMessage.deserialize(message)
        } catch (e: Throwable) {
            return
        }
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

            RedisMessage.TYPE_SEND_TOAST -> {
                // server:redpacket
                submit {
                    for (player in Bukkit.getOnlinePlayers()) {
                        val profile = PlayerProfile.read(player)
                        if (!profile.animation) {
                            continue
                        }
                        Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            "iaplaytotemanimation ${redisMessage.message} ${player.name}"
                        )
                    }
                }
            }
        }
    }

    fun close() {
        isSubscribed = false
    }

}