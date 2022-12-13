package com.xbaimiao.luochuan.redpacket.redis

import com.xbaimiao.easylib.info
import com.xbaimiao.easylib.submit
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisManager {

    private val channel = "LuoChuanRedPacket"

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

    @Synchronized
    fun createOrUpdate(redPacket: RedPacket) {
        jedisPool.resource.also {
            it.set(redPacket.toRedisKey(), RedPacket.serialize(redPacket))
            it.close()
        }
    }

    @Synchronized
    fun delete(id: String, async: Boolean = true) {
        if (!async) {
            synchronized(RedPacket.lock) {
                jedisPool.resource.also {
                    it.del(toRedisKey(id))
                    it.close()
                }
            }
            return
        }
        submit(async = true) {
            synchronized(RedPacket.lock) {
                jedisPool.resource.also {
                    it.del(toRedisKey(id))
                    it.close()
                }
            }
        }
    }

    @Synchronized
    private fun isLock(id: String): Boolean {
        jedisPool.resource.let {
            val result = it.exists("lock" + toRedisKey(id))
            it.close()
            return result
        }
    }

    @Synchronized
    private fun setLock(id: String, boolean: Boolean) {
        jedisPool.resource.let {
            if (boolean) {
                it.set("lock" + toRedisKey(id), "lock")
            } else {
                it.del("lock" + toRedisKey(id))
            }
            it.close()
        }
    }

    fun getRedPacket(id: String, func: RedPacket?.() -> Unit) {
        var max = 10
        Thread {
            synchronized(RedPacket.lock) {
                while (isLock(id)) {
                    Thread.sleep(500)
                    if (max-- <= 0) {
                        return@Thread
                    }
                }
                val redPacket = jedisPool.resource.let {
                    val redPacket = it.get(toRedisKey(id))
                    it.close()
                    redPacket
                } ?: return@Thread

                setLock(id, true)
                try {
                    val packet = RedPacket.deserialize(redPacket)
                    func.invoke(packet)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                setLock(id, false)
            }
        }.start()
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