package dev.mpa.client.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object SingBoxConfig {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    const val TUN_ADDRESS_V4 = "172.19.0.1/30"
    const val TUN_ADDRESS_V6 = "fdfe:dcba:9876::1/126"

    fun build(profile: ServerProfile): String {
        val isServerIp = isIpAddress(profile.address)

        val config = mapOf(
            "log" to mapOf("level" to "info", "timestamp" to true),

            "dns" to mapOf(
                "servers" to listOf(
                    mapOf("tag" to "dns-remote", "address" to "https://1.1.1.1/dns-query", "detour" to "proxy"),
                    mapOf("tag" to "dns-direct",  "address" to "local", "detour" to "direct")
                ),
                "rules" to buildList {
                    if (!isServerIp) add(mapOf("domain" to listOf(profile.address), "server" to "dns-direct"))
                },
                "final" to "dns-remote",
                "strategy" to "prefer_ipv4"
            ),

            "inbounds" to listOf(
                mapOf(
                    "type" to "tun",
                    "tag" to "tun-in",
                    "inet4_address" to TUN_ADDRESS_V4,
                    "inet6_address" to TUN_ADDRESS_V6,
                    "mtu" to 9000,
                    "auto_route" to false,
                    "stack" to "gvisor",
                    "sniff" to true,
                    "sniff_override_destination" to false
                )
            ),

            "outbounds" to listOf(
                mapOf(
                    "type" to "vless",
                    "tag" to "proxy",
                    "server" to profile.address,
                    "server_port" to profile.port,
                    "uuid" to profile.uuid,
                    "flow" to profile.flow.ifEmpty { null },
                    "tls" to mapOf(
                        "enabled" to true,
                        "server_name" to profile.serverName,
                        "utls" to mapOf("enabled" to true, "fingerprint" to profile.fingerprint.ifEmpty { "chrome" }),
                        "reality" to mapOf(
                            "enabled" to true,
                            "public_key" to profile.publicKey,
                            "short_id" to profile.shortId
                        )
                    )
                ),
                mapOf("type" to "direct", "tag" to "direct"),
                mapOf("type" to "dns",    "tag" to "dns-out")
            ),

            "route" to mapOf(
                "final" to "proxy",
                "rules" to listOf(
                    mapOf("protocol" to "dns", "outbound" to "dns-out"),
                    mapOf("ip_cidr" to PRIVATE_CIDRS, "outbound" to "direct")
                )
            )
        )

        return gson.toJson(config)
    }

    private fun isIpAddress(host: String) =
        host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) || host.contains(':')

    private val PRIVATE_CIDRS = listOf(
        "10.0.0.0/8", "127.0.0.0/8", "169.254.0.0/16",
        "172.16.0.0/12", "192.168.0.0/16",
        "224.0.0.0/4", "240.0.0.0/4",
        "::1/128", "fc00::/7", "fe80::/10"
    )
}
