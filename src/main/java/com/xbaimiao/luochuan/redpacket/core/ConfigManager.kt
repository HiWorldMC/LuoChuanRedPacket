package com.xbaimiao.luochuan.redpacket.core

import com.xbaimiao.easylib.module.utils.info
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.word.ContainWord
import com.xbaimiao.luochuan.redpacket.core.word.EqWord
import com.xbaimiao.luochuan.redpacket.core.word.Word

object ConfigManager {

    lateinit var redisHost: String
    var redisPort: Int = 6379
    var redisPassword: String? = null

    val words = mutableListOf<Word>()

    fun load() {
        redisHost = LuoChuanRedPacket.config.getString("redis.host")!!
        redisPort = LuoChuanRedPacket.config.getInt("redis.port")
        redisPassword = LuoChuanRedPacket.config.getString("redis.password")
        LuoChuanRedPacket.config.getStringList("blacklist").forEach {
            if (it.startsWith("*")) {
                words.add(ContainWord(it.substring(1)))
            } else {
                words.add(EqWord(it))
            }
        }
        info("加载了${words.size}个黑名单词")
    }

}