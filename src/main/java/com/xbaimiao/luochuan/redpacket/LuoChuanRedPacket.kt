package com.xbaimiao.luochuan.redpacket

import com.xbaimiao.luochuan.redpacket.command.OnCommand
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.redis.RedisManager
import top.mcplugin.lib.Plugin

@Suppress("unused")
class LuoChuanRedPacket : Plugin() {

    companion object {

        lateinit var redisManager: RedisManager

    }

    init {
        super.ignoreScan("shadow")
    }

    override fun enable() {
        saveDefaultConfig()
        ConfigManager.load()

        redisManager = RedisManager()
        redisManager.connect()

        RedPacketManager.load()
        getCommand("luochuanredpacket")!!.setExecutor(OnCommand())
    }

    override fun disable() {
        RedPacketManager.clear()
        redisManager.close()
    }

}