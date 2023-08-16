package com.xbaimiao.luochuan.redpacket

import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.luochuan.redpacket.command.OnCommand
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.core.listener.OnChat
import com.xbaimiao.luochuan.redpacket.data.PlayerProfile
import com.xbaimiao.luochuan.redpacket.redis.RedisManager
import org.bukkit.Bukkit

@Suppress("unused")
class LuoChuanRedPacket : EasyPlugin() {

    companion object {

        lateinit var redisManager: RedisManager
        val config get() = getPlugin<LuoChuanRedPacket>().config
    }

    override fun enable() {
        saveDefaultConfig()
        ConfigManager.load()

        redisManager = RedisManager()
        redisManager.connect()

        RedPacketManager.load()
        OnCommand.rootCommand.register()
        Bukkit.getPluginManager().registerEvents(OnChat(), this)

        PlayerProfile.connect()
    }

    override fun disable() {
        RedPacketManager.clear()
        redisManager.close()
    }

}