package com.xbaimiao.luochuan.redpacket.core.redpacket

import com.xbaimiao.easylib.chat.Lang
import java.util.*

abstract class AbstractRedPacket : RedPacket {

    companion object {
        private val random = Random()
    }

    override fun toMessage(): String {
        return Lang.asLangText("redpacket.send-common", sender, totalMoney)
    }

    fun initPackNum(totalAmount: Int, totalPeopleNum: Int): ArrayList<Int> {
        val amountList = ArrayList<Int>()
        var restAmount = totalAmount
        var restPeopleAmount = totalPeopleNum
        for (i in 0 until totalPeopleNum - 1) {
            val amount: Int = random.nextInt(restAmount / restPeopleAmount * 2 - 1) + 1
            restAmount -= amount
            restPeopleAmount--
            amountList.add(amount)
        }
        amountList.add(restAmount)
        return amountList
    }

}
