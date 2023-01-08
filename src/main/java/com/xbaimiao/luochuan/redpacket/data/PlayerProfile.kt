package com.xbaimiao.luochuan.redpacket.data

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import org.bukkit.entity.Player
import top.mcplugin.lib.Plugin
import top.mcplugin.lib.module.ormlite.OrmliteMysql
import java.util.concurrent.CompletableFuture

@DatabaseTable(tableName = "mysticredpacket_player")
class PlayerProfile {

    @DatabaseField(id = true)
    lateinit var player: String

    @DatabaseField
    var animation: Boolean = false

    companion object {

        private lateinit var profileDao: Dao<PlayerProfile, String>

        fun connect() {
            val section = Plugin.getPlugin().config.getConfigurationSection("mysql")
            if (section == null) {
                Plugin.getPlugin().logger.warning("未配置mysql数据库")
                return
            }
            val ormlite = OrmliteMysql(section, true)
            profileDao = ormlite.createDao(PlayerProfile::class.java)
        }

        fun read(player: Player): CompletableFuture<PlayerProfile> {
            return CompletableFuture.supplyAsync {
                profileDao.queryForId(player.name) ?: PlayerProfile().apply {
                    this.player = player.name
                    this.animation = true
                    profileDao.create(this)
                }
            }
        }

        fun save(profile: PlayerProfile): CompletableFuture<Dao.CreateOrUpdateStatus> {
            return CompletableFuture.supplyAsync {
                profileDao.createOrUpdate(profile)
            }
        }

    }

}