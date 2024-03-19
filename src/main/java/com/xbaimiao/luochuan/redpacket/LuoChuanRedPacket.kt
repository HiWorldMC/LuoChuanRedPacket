package com.xbaimiao.luochuan.redpacket

import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.util.plugin
import com.xbaimiao.ktor.KtorPluginsBukkit
import com.xbaimiao.ktor.KtorStat
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.core.listener.OnChat
import com.xbaimiao.luochuan.redpacket.data.PlayerProfile
import com.xbaimiao.luochuan.redpacket.redis.RedisManager
import org.bukkit.Bukkit

@Suppress("unused")
class LuoChuanRedPacket : EasyPlugin(), KtorStat {

    companion object {

        lateinit var redisManager: RedisManager
        val config get() = plugin.config
    }

    override fun enable() {
        // 初始化
        KtorPluginsBukkit.init(this, this)
        // 获取下载此插件的用户id 如果没有注入用户id 则会抛出异常 NoInjectionException
        val user = kotlin.runCatching { super.getUserId() }.getOrNull()
        if (user != null) {
            // 接入插件在线统计
            stat()
        }
        saveDefaultConfig()
        ConfigManager.load(config)

        redisManager = RedisManager()
        redisManager.connect()

        RedPacketManager.load()
        rootCommand.register()
        Bukkit.getPluginManager().registerEvents(OnChat(), this)

        PlayerProfile.connect()
    }

    override fun disable() {
        RedPacketManager.clear()
        redisManager.close()
    }

}
