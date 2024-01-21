package com.xbaimiao.luochuan.redpacket.core

import com.xbaimiao.easylib.util.info
import com.xbaimiao.luochuan.redpacket.core.word.ContainWord
import com.xbaimiao.luochuan.redpacket.core.word.EqWord
import com.xbaimiao.luochuan.redpacket.core.word.Word
import org.bukkit.configuration.Configuration

object ConfigManager {

    lateinit var redisHost: String
    var redisPort: Int = 6379
    var redisPassword: String? = null

    val words = mutableListOf<Word>()

    fun load(config: Configuration) {
        redisHost = config.getString("redis.host") ?: error("未找到 redis.host")
        redisPort = config.getInt("redis.port")
        redisPassword = config.getString("redis.password")
        config.getStringList("blacklist").forEach {
            if (it.startsWith("*")) {
                words.add(ContainWord(it.substring(1)))
            } else {
                words.add(EqWord(it))
            }
        }
        info("加载了${words.size}个黑名单词")
    }

}
