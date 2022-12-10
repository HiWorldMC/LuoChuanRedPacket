package com.xbaimiao.luochuan.redpacket.core

import com.xbaimiao.easylib.submit
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket

object RedPacketManager {

    private val redPacket = HashMap<Long, RedPacket>()

    fun load() {
        submit(async = true, period = 200) {
            synchronized(redPacket) {
                // 删除过期数据
                val deleteList = ArrayList<Long>()
                redPacket.forEach {
                    if (it.key < System.currentTimeMillis()) {
                        delete(it.value)
                        deleteList.add(it.key)
                    }
                }
                deleteList.forEach {
                    redPacket.remove(it)
                }
            }
        }
    }

    fun addRedPacket(redPacket: RedPacket) {
        synchronized(redPacket) {
            this.redPacket[System.currentTimeMillis() + 1000 * 60 * 30] = redPacket
        }
    }

    fun clear() {
        synchronized(redPacket) {
            redPacket.forEach { delete(it.value) }
        }
    }

    private fun delete(redPacket: RedPacket) {
        LuoChuanRedPacket.redisManager.delete(redPacket.id)
    }

}