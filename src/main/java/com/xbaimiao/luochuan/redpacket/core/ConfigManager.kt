package com.xbaimiao.luochuan.redpacket.core

import top.mcplugin.lib.Plugin

object ConfigManager {

    lateinit var redisHost: String
    var redisPort: Int = 6379
    var redisPassword: String? = null

    fun load() {
        redisHost = Plugin.getPlugin().config.getString("redis.host")!!
        redisPort = Plugin.getPlugin().config.getInt("redis.port")
        redisPassword = Plugin.getPlugin().config.getString("redis.password")
    }

}