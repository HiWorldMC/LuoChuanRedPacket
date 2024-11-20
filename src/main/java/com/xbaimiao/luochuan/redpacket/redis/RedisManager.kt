package com.xbaimiao.luochuan.redpacket.redis

import com.xbaimiao.easylib.util.DistributedLock
import com.xbaimiao.easylib.util.buildDistributedLock
import com.xbaimiao.easylib.util.info
import com.xbaimiao.easylib.util.submit
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import org.bukkit.entity.Player
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class RedisManager {

    private val channel = "LuoChuanRedPacket"

    private lateinit var jedisPool: JedisPool
    private lateinit var subscribeThread: Thread
    private lateinit var distributedLock: DistributedLock
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
        jedisPool.resource.use {
            it.set(redPacket.toRedisKey(), RedPacket.serialize(redPacket))
        }
    }

    @Synchronized
    fun removeTextRedPacket(id: String) {
        jedisPool.resource.also {
            it.del(toRedisKey("$id:text"))
            it.close()
        }
    }

    @Synchronized
    fun addTextRedPacket(redPacket: RedPacket, text: String) {
        jedisPool.resource.also {
            it.set(redPacket.toRedisKey(), RedPacket.serialize(redPacket))
            it.set(toRedisKey(redPacket.id + ":text"), text)
            it.close()
        }
    }

    @Synchronized // id: text
    fun getTextRedPackets(): List<Pair<String, String>> {
        val list = ArrayList<Pair<String, String>>()
        jedisPool.resource.also { jedis ->
            jedis.keys("$channel:*:text").forEach {
                list.add(Pair(it, jedis.get(it)))
            }
            jedis.close()
        }
        return list
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

    private val gettingSet = CopyOnWriteArraySet<String>()

    fun canGetRedPacket(player: Player, id: String): Boolean {
        return !gettingSet.contains(player.name + id)
    }

    fun getRedPacket(player: Player, id: String, func: RedPacket?.() -> Unit) {
        submit(async = true) {
            gettingSet.add(player.name + id)
            distributedLock.withLock(id) {
                val redPacket = jedisPool.resource.use { it.get(toRedisKey(id)) }
                if (redPacket == null) {
                    func.invoke(null)
                    return@withLock
                }
                try {
                    val packet = RedPacket.deserialize(redPacket)
                    val future = CompletableFuture<Any>()
                    submit {
                        try {
                            func.invoke(packet)
                            future.complete(null)
                        } catch (t: Throwable) {
                            future.complete(null)
                        }
                    }
                    future.get(10, TimeUnit.SECONDS)
                } catch (t: Throwable) {
                    t.printStackTrace()
                } finally {
                    // done
                    gettingSet.remove(player.name + id)
                }
            }
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
        distributedLock = buildDistributedLock(jedisPool)

        info("Redis连接成功")
    }

    @Suppress("DEPRECATION")
    fun close() {
        cancel = true
        subscribe.close()
        if (this::subscribeThread.isInitialized) {
            this.subscribeThread.interrupt()
        }
        if (this::jedisPool.isInitialized) {
            jedisPool.destroy()
        }
    }

}
