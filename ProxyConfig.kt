package com.xboard.app.vpn

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ProxyConfig {

    fun build(node: VpnNode): String = buildJsonObject {
        put(
            "log",
            buildJsonObject {
                put("level", "warn")
                put("timestamp", true)
            },
        )
        put(
            "dns",
            buildJsonObject {
                put(
                    "servers",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("tag", "dns-remote")
                                put("address", "https://1.1.1.1/dns-query")
                                put("detour", "proxy")
                                put("strategy", "prefer_ipv4")
                            },
                        )
                        add(
                            buildJsonObject {
                                put("tag", "dns-local")
                                put("address", "223.5.5.5")
                                put("detour", "direct")
                                put("strategy", "prefer_ipv4")
                            },
                        )
                    },
                )
                put("final", "dns-remote")
                put("independent_cache", true)
            },
        )
        put(
            "inbounds",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", "tun")
                        put("tag", "tun-in")
                        put("inet4_address", buildJsonArray { add("172.19.0.1/30") })
                        put("mtu", 9000)
                        put("auto_route", true)
                        put("strict_route", false)
                        put("stack", "system")
                        put("sniff", true)
                    },
                )
            },
        )
        put(
            "outbounds",
            buildJsonArray {
                add(node.outbound)
                add(
                    buildJsonObject {
                        put("type", "direct")
                        put("tag", "direct")
                    },
                )
            },
        )
        put(
            "route",
            buildJsonObject {
                put("final", "proxy")
                put("auto_detect_interface", true)
            },
        )
    }.toString()
}
