package com.xbaimiao.luochuan.redpacket.core.redpacket

data class CommonRedPacket(
    override val id: String,
    override val totalMoney: Double,
    override val totalNum: Int,
    override val remainMoney: Double,
    override val remainNum: Int,
    override val sender: String
) : RedPacket {

    companion object : RedPacketSerializer {

        private const val KEY = "CommonRedPacket"

        override fun serialize(redPacket: RedPacket): String? {
            if (redPacket !is CommonRedPacket) {
                return null
            }
            return KEY + redPacket.id + "," + redPacket.totalMoney + "," + redPacket.totalNum + "," + redPacket.remainMoney + "," + redPacket.remainNum + "," + redPacket.sender
        }

        override fun deserialize(string: String): RedPacket? {
            if (!string.startsWith(KEY)) {
                return null
            }
            val split = string.substring(KEY.length).split(",")
            if (split.size != 6) {
                return null
            }
            return CommonRedPacket(
                split[0],
                split[1].toDouble(),
                split[2].toInt(),
                split[3].toDouble(),
                split[4].toInt(),
                split[5]
            )
        }

    }

}