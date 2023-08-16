package com.xbaimiao.luochuan.redpacket.command

import com.xbaimiao.easylib.bridge.economy.EconomyManager
import com.xbaimiao.easylib.module.chat.Lang.sendLang
import com.xbaimiao.easylib.module.command.ArgNode
import com.xbaimiao.easylib.module.command.CommandContext
import com.xbaimiao.easylib.module.command.command
import com.xbaimiao.easylib.module.utils.submit
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import com.xbaimiao.luochuan.redpacket.core.ConfigManager
import com.xbaimiao.luochuan.redpacket.core.RedPacketManager
import com.xbaimiao.luochuan.redpacket.core.redpacket.CommonRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.PointsRedPacket
import com.xbaimiao.luochuan.redpacket.core.redpacket.RedPacket
import com.xbaimiao.luochuan.redpacket.data.PlayerProfile
import com.xbaimiao.luochuan.redpacket.redis.RedisMessage
import com.xbaimiao.luochuan.redpacket.serialize
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

object OnCommand {

    private val moneyArgNode = ArgNode("红包金额", exec = { listOf("整数") }, parse = { it.toIntOrNull() })
    private val numArgNode = ArgNode("红包数量", exec = { listOf("整数") }, parse = { it.toIntOrNull() })
    private val textArgNode = ArgNode("红包口令", exec = { token ->
        Bukkit.getOnlinePlayers().map { it.name }.map { "${it}是猪" }
            .filter { it.uppercase().startsWith(token.uppercase()) }
    }, parse = { it })

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

    private val sendVault = command<Player>("send-vault") {
        description = "发送金币红包"
        permission = "luochuanredpacket.command.vault"
        arg(moneyArgNode) { moneyArg ->
            arg(numArgNode) { numArg ->
                exec {
                    val data = check(this, moneyArg, numArg, null) ?: return@exec error("参数错误")

                    if (!EconomyManager.vault.has(sender, data.money.toDouble())) {
                        sender.sendLang("redpacket.send-no-money")
                        return@exec
                    }
                    EconomyManager.vault.take(sender, data.money.toDouble())

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

    private val sendPoints = command<Player>("sendPoints") {
        description = "发送点券红包"
        permission = "luochuanredpacket.command.points"
        arg(moneyArgNode) { moneyArg ->
            arg(numArgNode) { numArg ->
                exec {
                    val data = check(this, moneyArg, numArg, null) ?: return@exec error("参数错误")
                    if (!EconomyManager.playerPoints.has(sender, data.money.toDouble())) {
                        sender.sendLang("redpacket.send-no-money-points")
                        return@exec
                    }
                    EconomyManager.playerPoints.take(sender, data.money.toDouble())

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

    private val sendTextVault = command<Player>("sendTextVault") {
        description = "发送口令金币红包"
        permission = "luochuanredpacket.command.vault.text"
        arg(moneyArgNode) { moneyArg ->
            arg(numArgNode) { numArg ->
                arg(textArgNode) { textArg ->
                    exec {
                        val data = check(this, moneyArg, numArg, textArg) ?: return@exec error("参数错误")
                        if (data.text == null) {
                            sender.sendLang("redpacket.send-text-value-null")
                            return@exec
                        }

                        if (!ConfigManager.words.any { it.canSend(data.text) }) {
                            sender.sendLang("command.not-pass")
                            return@exec
                        }

                        if (!EconomyManager.vault.has(sender, data.money.toDouble())) {
                            sender.sendLang("redpacket.send-no-money")
                            return@exec
                        }
                        EconomyManager.vault.take(sender, data.money.toDouble())

                        val redPacket = CommonRedPacket(
                            UUID.randomUUID().toString().replace("-", ""),
                            data.money,
                            data.num,
                            data.money,
                            data.num,
                            sender.name
                        )
                        sendText(sender, redPacket, data.text)
                    }
                }
            }
        }
    }

    private val sendTextPoints = command<Player>("sendTextPoints") {
        description = "发送口令点券红包"
        permission = "luochuanredpacket.command.points.text"
        arg(moneyArgNode) { moneyArg ->
            arg(numArgNode) { numArg ->
                arg(textArgNode) { textArg ->
                    exec {
                        val data = check(this, moneyArg, numArg, textArg) ?: return@exec error("参数错误")

                        if (data.text == null) {
                            sender.sendLang("redpacket.send-text-value-null")
                            return@exec
                        }

                        if (!ConfigManager.words.any { it.canSend(data.text) }) {
                            sender.sendLang("command.not-pass")
                            return@exec
                        }

                        if (!EconomyManager.playerPoints.has(sender, data.money.toDouble())) {
                            sender.sendLang("redpacket.send-no-money-points")
                            return@exec
                        }
                        EconomyManager.playerPoints.take(sender, data.money.toDouble())

                        val redPacket = PointsRedPacket(
                            UUID.randomUUID().toString().replace("-", ""),
                            data.money,
                            data.num,
                            data.money,
                            data.num,
                            sender.name
                        )
                        sendText(sender, redPacket, data.text)
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

            val component =
                redPacket.toComponent().clickEvent(ClickEvent.runCommand("/luochuanredpacket get ${redPacket.id}"))
                    .hoverEvent(HoverEvent.showText(Component.text("点击领取")))

            LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_PACKET, component.serialize()))
            LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_SEND_TOAST, "server:redpacket"))
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
            LuoChuanRedPacket.redisManager.push(RedisMessage(RedisMessage.TYPE_SEND_TOAST, "server:redpacket"))
        }
    }

    private fun check(
        context: CommandContext<*>,
        moneyArg: ArgNode<Int?>,
        numArg: ArgNode<Int?>,
        textArg: ArgNode<String>?
    ): SendData? {

        val money = context.valueOfOrNull(moneyArg)
        val num = context.valueOfOrNull(numArg)

        if (money == null || num == null) {
            context.sender.sendLang("command.not-number")
            return null
        }

        if (money < 1 || num < 1) {
            context.sender.sendLang("command.number-too-small")
            return null
        }

        if (money < num) {
            context.sender.sendLang("redpacket.money-less-than-num")
            return null
        }
        val text = if (textArg == null) null else context.valueOfOrNull(textArg)
        return SendData(money, num, text)
    }

    data class SendData(val money: Int, val num: Int, val text: String?)

}

