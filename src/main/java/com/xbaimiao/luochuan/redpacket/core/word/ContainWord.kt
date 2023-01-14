package com.xbaimiao.luochuan.redpacket.core.word

class ContainWord(
    val text: String
) : Word {

    override fun canSend(text: String): Boolean {
        return !text.contains(this.text)
    }

}