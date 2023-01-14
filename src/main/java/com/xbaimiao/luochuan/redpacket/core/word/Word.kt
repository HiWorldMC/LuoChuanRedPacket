package com.xbaimiao.luochuan.redpacket.core.word

// 黑名单词
interface Word {

    fun canSend(text: String): Boolean

}