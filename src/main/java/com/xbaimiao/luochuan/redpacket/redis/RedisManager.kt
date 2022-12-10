package com.xbaimiao.luochuan.redpacket.redis

import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import top.mcplugin.lib.Plugin

class RedisManager {

    private val channel = "LuoChuanRedPacket2"

    private lateinit var jedisPool: JedisPool

    private lateinit var subscribeThread: Thread
    private var cancel = false

    fun push(message: String) {
        jedisPool.resource.publish(channel, message)
    }

    fun createOrUpdate(redPacket: RedPacket) {
        jedisPool.resource.set("$channel${redPacket.id}", RedPacket.serialize(redPacket))
    }

    fun getRedPacket(id: String): RedPacket? {
        val jedis = jedisPool.resource
        val string = jedis.get("$channel$id")
        return if (string == null) {
            null
        } else {
            RedPacket.deserialize(string)
        }
    }

    fun connect() {
        cancel = false
        jedisPool = if (ConfigManager.redisPassword != null) {
            JedisPool(
                JedisPoolConfig(), ConfigManager.redisHost, ConfigManager.redisPort, 2000, ConfigManager.redisPassword
            )
        } else {
            JedisPool(ConfigManager.redisHost, ConfigManager.redisPort)
        }

        subscribeThread = Thread {
            jedisPool.resource.subscribe(OnRedisMessage(), channel)
        }
        subscribeThread.start()

        Plugin.getPlugin().logger.info("Redis连接成功")
    }

    fun close() {
        cancel = true
        if (this::subscribeThread.isInitialized) {
            this.subscribeThread.interrupt()
            this.subscribeThread.stop()
        }
        if (this::jedisPool.isInitialized) {
            jedisPool.destroy()
        }
    }

}