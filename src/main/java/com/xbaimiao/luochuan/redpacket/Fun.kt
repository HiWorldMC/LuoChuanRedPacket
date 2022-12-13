package com.xbaimiao.luochuan.redpacket

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

fun Component.serialize(): String = GsonComponentSerializer.gson().serialize(this)