package com.xbaimiao.luochuan.redpacket

import com.xbaimiao.easylib.bridge.economy.Economy
import com.xbaimiao.easylib.bridge.economy.EconomyManager
import com.xbaimiao.easylib.chat.Lang.sendLang
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.CommandContext
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.util.plugin
import com.xbaimiao.easylib.util.submit
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.CommonRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.PointsRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import com.xbaimiao.luochuan.redpacket.data.PlayerProfile
import com.xbaimiao.luochuan.redpacket.redis.RedisMessage
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

private val moneyArgNode = ArgNode("红包金额", exec = { listOf("整数") }, parse = { it.toIntOrNull() })
private val numArgNode = ArgNode("红包数量", exec = { listOf("整数") }, parse = { it.toIntOrNull() })
private val textArgNode = ArgNode("红包口令", exec = { token ->
    Bukkit.getOnlinePlayers().map { it.name }.map { "${it}是猪" }
        .filter { it.uppercase().startsWith(token.uppercase()) }
}, parse = { it })

private val forceArgNode = ArgNode("是否强制发送", exec = { listOf("true", "false") }, parse = { it.toBoolean() })
private val maxMoney get() = LuoChuanRedPacket.config.getInt("maxMoney", Int.MAX_VALUE)
private val minMoney get() = LuoChuanRedPacket.config.getInt("minMoney", 1)
private val maxPoints get() = LuoChuanRedPacket.config.getInt("maxPoint", Int.MAX_VALUE)
private val minPoints get() = LuoChuanRedPacket.config.getInt("minPoints", 1)


private val toggleCommand = command<Player>("toggle") {
    description = "开启/关闭红包动画"
    exec {
        PlayerProfile.read(sender).thenAcceptAsync { profile ->
            profile.animation = !profile.animation
            PlayerProfile.save(profile)
            sender.sendLang("redpacket.toggle", if (profile.animation) "开启" else "关闭")
        }
    }
}

private val get = command<Player>("get") {
    description = "领取红包命令"
    arg("红包ID") { passwdArg ->
        exec {
            val id = valueOf(passwdArg)
            LuoChuanRedPacket.redisManager.getRedPacket(id) {
                try {
                    if (this == null) {
                        this@exec.sender.sendLang("redpacket.not-exist")
                    } else {
                        this.send(this@exec.sender)
                        LuoChuanRedPacket.redisManager.createOrUpdate(this)
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
        }
    }
}

private val sendVault = command<CommandSender>("send-vault") {
    description = "发送金币红包"
    permission = "luochuanredpacket.command.vault"
    arg(moneyArgNode) { moneyArg ->
        arg(numArgNode) { numArg ->
            arg(forceArgNode, optional = true) { forceArg ->
                exec {
                    val data = check(this, moneyArg, numArg, null, maxMoney, minMoney) ?: return@exec error("参数错误")

                    if (!checkForceAndEconomy(this, forceArg, EconomyManager.vault, data)) {
                        return@exec
                    }

                    val redPacket = CommonRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money,
                        data.num,
                        data.money,
                        data.num,
                        sender.name
                    )
                    send(redPacket)
                }
            }
        }
    }
}

private val sendPoints = command<CommandSender>("sendPoints") {
    description = "发送点券红包"
    permission = "luochuanredpacket.command.points"
    arg(moneyArgNode) { moneyArg ->
        arg(numArgNode) { numArg ->
            arg(forceArgNode, optional = true) { forceArg ->
                exec {
                    val data =
                        check(this, moneyArg, numArg, null, maxPoints, minPoints) ?: return@exec error("参数错误")

                    if (!checkForceAndEconomy(this, forceArg, EconomyManager.playerPoints, data)) {
                        return@exec
                    }

                    val redPacket = PointsRedPacket(
                        UUID.randomUUID().toString().replace("-", ""),
                        data.money,
                        data.num,
                        data.money,
                        data.num,
                        sender.name
                    )
                    send(redPacket)
                }
            }
        }
    }
}

private val sendTextVault = command<CommandSender>("sendTextVault") {
    description = "发送口令金币红包"
    permission = "luochuanredpacket.command.vault.text"
    arg(moneyArgNode) { moneyArg ->
        arg(numArgNode) { numArg ->
            arg(textArgNode) { textArg ->
                arg(forceArgNode, optional = true) { forceArg ->

                    exec {
                        val data =
                            check(this, moneyArg, numArg, textArg, maxMoney, minMoney) ?: return@exec error("参数错误")

                        if (!checkText(this, data)) {
                            return@exec
                        }

                        if (!checkForceAndEconomy(this, forceArg, EconomyManager.vault, data)) {
                            return@exec
                        }

                        val redPacket = CommonRedPacket(
                            UUID.randomUUID().toString().replace("-", ""),
                            data.money,
                            data.num,
                            data.money,
                            data.num,
                            sender.name
                        )
                        sendText(sender, redPacket, data.text!!)
                    }
                }
            }
        }
    }
}

private val sendTextPoints = command<CommandSender>("sendTextPoints") {
    description = "发送口令点券红包"
    permission = "luochuanredpacket.command.points.text"
    arg(moneyArgNode) { moneyArg ->
        arg(numArgNode) { numArg ->
            arg(textArgNode) { textArg ->
                arg(forceArgNode, optional = true) { forceArg ->

                    exec {
                        val data = check(this, moneyArg, numArg, textArg, maxPoints, minPoints)
                            ?: return@exec error("参数错误")

                        if (!checkText(this, data)) {
                            return@exec
                        }

                        if (!checkForceAndEconomy(this, forceArg, EconomyManager.playerPoints, data)) {
                            return@exec
                        }

                        val redPacket = PointsRedPacket(
                            UUID.randomUUID().toString().replace("-", ""),
                            data.money,
                            data.num,
                            data.money,
                            data.num,
                            sender.name
                        )
                        sendText(sender, redPacket, data.text!!)
                    }
                }
            }
        }
    }
}

val rootCommand = command<CommandSender>("luochuanredpacket") {
    description = "主命令"
    sub(toggleCommand)
    sub(get)
    sub(sendVault)
    sub(sendPoints)
    sub(sendTextVault)
    sub(sendTextPoints)
}

private fun send(redPacket: RedPacket) {
    submit(async = true) {
        LuoChuanRedPacket.redisManager.createOrUpdate(redPacket)
        RedPacketManager.addRedPacket(redPacket)

        LuoChuanRedPacket.redisManager.push(RedisMessage.typePacket(redPacket.toMessage(), true, redPacket.id))

        val animation = if (redPacket is PointsRedPacket) {
            LuoChuanRedPacket.config.getString("animation.point")
        } else {
            LuoChuanRedPacket.config.getString("animation.vault")
        } ?: "server:redpacket"

        LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_SEND_TOAST, animation))
    }
}

private fun sendText(sender: CommandSender, redPacket: RedPacket, text: String) {
    submit(async = true) {
        if (!ConfigManager.words.any { it.canSend(text) }) {
            sender.sendLang("command.not-pass")
            return@submit
        }
        LuoChuanRedPacket.redisManager.createOrUpdate(redPacket)
        LuoChuanRedPacket.redisManager.addTextRedPacket(redPacket, text)
        RedPacketManager.addRedPacket(redPacket)

        LuoChuanRedPacket.redisManager.push(
            RedisMessage(
                RedisMessage.TYPE_BC, redPacket.getTextRedPackMessage(text)
            )
        )

        val animation = if (redPacket is PointsRedPacket) {
            LuoChuanRedPacket.config.getString("animation.point")
        } else {
            LuoChuanRedPacket.config.getString("animation.vault")
        } ?: "server:redpacket"
        LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_SEND_TOAST, animation))
    }
}

/**
 * 检查命令参数并返回数据
 */
private fun check(
    context: CommandContext<*>,
    moneyArg: ArgNode<Int?>,
    numArg: ArgNode<Int?>,
    textArg: ArgNode<String>?,
    maxMoney: Int,
    minMoney: Int
): SendData? {

    val money = context.valueOfOrNull(moneyArg)
    val num = context.valueOfOrNull(numArg)

    if (money == null || num == null) {
        context.sender.sendLang("command.not-number")
        return null
    }

    if (money > maxMoney) {
        context.sender.sendLang("redpacket.money-too-big")
        return null
    }

    if (money < minMoney || num < 1) {
        context.sender.sendLang("command.number-too-small")
        return null
    }
    if (num < plugin.config.getInt("minSize", 0)) {
        context.sender.sendLang("redpacket.minSize")
        return null
    }

    if (money < num) {
        context.sender.sendLang("redpacket.money-less-than-num")
        return null
    }
    val text = if (textArg == null) null else context.valueOfOrNull(textArg)
    return SendData(money, num, text)
}

/**
 * 检查口令红包的口令是否能发送
 */
private fun checkText(context: CommandContext<*>, data: SendData): Boolean {
    if (data.text == null) {
        context.sender.sendLang("redpacket.send-text-value-null")
        return false
    }

    if (!ConfigManager.words.any { it.canSend(data.text) }) {
        context.sender.sendLang("command.not-pass")
        return false
    }
    return true
}

/**
 * 检查玩家强制执行的权限 和是否有足够的货币
 */
private fun checkForceAndEconomy(
    context: CommandContext<*>, forceArg: ArgNode<Boolean>, economy: Economy, data: SendData
): Boolean {
    val force = context.valueOfOrNull(forceArg) ?: false

    // 如果想强制发送 但执行者不是OP
    if (force && !context.sender.isOp) {
        context.sender.sendLang("command.no-permission")
        return false
    }

    if (!force) {
        val player = context.sender as? Player
        if (player == null) {
            context.sender.sendLang("command.force-only-player")
            return false
        }
        if (!economy.has(player, data.money.toDouble())) {
            context.sender.sendLang("redpacket.send-no-money-points")
            return false
        }
        economy.take(player, data.money.toDouble())
    }
    return true
}

data class SendData(val money: Int, val num: Int, val text: String?)
