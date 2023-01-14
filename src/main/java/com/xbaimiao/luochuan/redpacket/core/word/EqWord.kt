package com.xbaimiao.luochuan.redpacket.core.word

class EqWord(
    val text: String
) : Word {

    override fun canSend(text: String): Boolean {
        return text != this.text
    }

}
