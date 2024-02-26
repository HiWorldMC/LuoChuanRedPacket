package com.xbaimiao.luochuan.redpacket.redis

import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.util.info
import com.xbaimiao.easylib.util.submit
import com.xbaimiao.luochuan.redpacket.data.PlayerProfile
import com.xbaimiao.luochuan.redpacket.redis.message.PlayerMessage
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
                val args = redisMessage.message.split(RedisMessage.SPACE)

                val rawMessage = args[0]
                val canGet = args[1].toBoolean()
                val id = args[2]

                val tellrawJson = TellrawJson()

                tellrawJson.broadcast {
                    append(rawMessage)
                    if (canGet) {
                        runCommand("/luochuanredpacket get $id")
                        hoverText("点击领取")
                    }
                }
            }

            RedisMessage.TYPE_BC -> {
                Bukkit.getOnlinePlayers().forEach {
                    it.sendMessage(redisMessage.message)
                }
            }

            RedisMessage.TYPE_SEND_MESSAGE -> {
                val data = PlayerMessage.deserialize(redisMessage.message)
                val player = Bukkit.getPlayerExact(data.player) ?: return
                player.sendMessage(data.message)
            }

            RedisMessage.TYPE_SEND_TOAST -> {
                // server:redpacket
                for (player in Bukkit.getOnlinePlayers()) {
                    PlayerProfile.read(player).thenAccept { profile ->
                        if (!profile.animation) {
                            return@thenAccept
                        }
                        submit {
                            Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                "iaplaytotemanimation ${redisMessage.message} ${player.name}"
                            )
                        }
                    }
                }
            }
        }
    }

    fun close() {
        isSubscribed = false
    }

}
