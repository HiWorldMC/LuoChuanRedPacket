package com.xbaimiao.luochuan.redpacket.data

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.xbaimiao.easylib.module.database.OrmliteMysql
import com.xbaimiao.easylib.module.database.OrmliteSQLite
import com.xbaimiao.easylib.module.utils.warn
import com.xbaimiao.luochuan.redpacket.LuoChuanRedPacket
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

@DatabaseTable(tableName = "luochuanredpacket_player")
class PlayerProfile {

    @DatabaseField(id = true)
    lateinit var player: String

    @DatabaseField
    var animation: Boolean = false

    companion object {

        private lateinit var profileDao: Dao<PlayerProfile, String>

        fun connect() {
            val section = LuoChuanRedPacket.config.getConfigurationSection("mysql")
            if (section == null) {
                warn("未配置mysql数据库")
                return
            }
            val ormlite = if (section.getBoolean("mysql.enable", true)) {
                OrmliteMysql(section, true)
            } else {
                OrmliteSQLite("database.db")
            }
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