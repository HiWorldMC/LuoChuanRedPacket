package com.xbaimiao.luochuan.redpacket.core

import com.xbaimiao.easylib.info
import com.xbaimiao.luochuan.redpacket.core.word.ContainWord
import com.xbaimiao.luochuan.redpacket.core.word.EqWord
import com.xbaimiao.luochuan.redpacket.core.word.Word
import top.mcplugin.lib.Plugin

object ConfigManager {

    lateinit var redisHost: String
    var redisPort: Int = 6379
    var redisPassword: String? = null

    val words = mutableListOf<Word>()

    fun load() {
        redisHost = Plugin.getPlugin().config.getString("redis.host")!!
        redisPort = Plugin.getPlugin().config.getInt("redis.port")
        redisPassword = Plugin.getPlugin().config.getString("redis.password")
        Plugin.getPlugin().config.getStringList("blacklist").forEach {
            if (it.startsWith("*")) {
                words.add(ContainWord(it.substring(1)))
            } else {
                words.add(EqWord(it))
            }
        }
        info("加载了${words.size}个黑名单词")
    }

}