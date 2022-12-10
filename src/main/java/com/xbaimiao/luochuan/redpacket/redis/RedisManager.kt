package com.xbaimiao.luochuan.redpacket.redis

import com.xbaimiao.easylib.info
import com.xbaimiao.easylib.submit
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.concurrent.CompletableFuture

class RedisManager {

    private val channel = "LuoChuanRedPacket4"

    private lateinit var jedisPool: JedisPool

    private lateinit var subscribeThread: Thread
    private var cancel = false
    private val subscribe = OnRedisMessage()

    private fun RedPacket.toRedisKey(): String {
        return toRedisKey(id)
    }

    private fun toRedisKey(id: String): String {
        return "$channel:${id}"
    }

    fun push(message: RedisMessage) {
        submit(async = true) {
            jedisPool.resource.also {
                it.publish(channel, message.serialize())
                it.close()
            }
        }
    }

    fun createOrUpdate(redPacket: RedPacket) {
        submit(async = true) {
            synchronized(RedPacket.lock) {
                jedisPool.resource.also {
                    it.set(redPacket.toRedisKey(), RedPacket.serialize(redPacket))
                    it.close()
                }
            }
        }
    }

    fun delete(id: String) {
        submit(async = true) {
            synchronized(RedPacket.lock) {
                jedisPool.resource.also {
                    it.del(toRedisKey(id))
                    it.close()
                }
            }
        }
    }

    fun getRedPacket(id: String): CompletableFuture<RedPacket?> {
        return CompletableFuture<RedPacket?>().also {
            Thread {
                synchronized(RedPacket.lock) {
                    val redPacket = jedisPool.resource.let {
                        val redPacket = it.get(toRedisKey(id))
                        it.close()
                        redPacket
                    }
                    if (redPacket == null) {
                        it.complete(null)
                    }
                    it.complete(RedPacket.deserialize(redPacket))
                }
            }.start()
        }
    }

    fun connect() {
        cancel = false
        val config = JedisPoolConfig()
        config.maxTotal = 10
        jedisPool = if (ConfigManager.redisPassword != null) {
            JedisPool(
                config, ConfigManager.redisHost, ConfigManager.redisPort, 2000, ConfigManager.redisPassword
            )
        } else {
            JedisPool(config, ConfigManager.redisHost, ConfigManager.redisPort)
        }

        subscribeThread = Thread {
            jedisPool.resource.subscribe(subscribe, channel)
        }
        subscribeThread.start()

        info("Redis连接成功")
    }

    @Suppress("DEPRECATION")
    fun close() {
        cancel = true
        subscribe.close()
        if (this::subscribeThread.isInitialized) {
            this.subscribeThread.interrupt()
            this.subscribeThread.stop()
        }
        if (this::jedisPool.isInitialized) {
            jedisPool.destroy()
        }
    }

}