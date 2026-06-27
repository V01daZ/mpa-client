package dev.mpa.client.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object SingBoxConfig {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    const val TUN_ADDRESS_V4 = "172.19.0.1/30"
    const val TUN_ADDRESS_V6 = "fdfe:dcba:9876::1/126"

    // Имена файлов rule-set которые RuleSetDownloader кладёт в filesDir
    const val GEOIP_RU_FILENAME    = "geoip-ru.srs"
    const val GEOSITE_RU_FILENAME  = "geosite-ru.srs"

    private val RU_DOMAINS = listOf(
        ".ru", ".su", ".рф", ".xn--p1ai" // Russia
    )

    /**
     * Строит конфиг sing-box.
     *
     * [rulesDir] — путь к директории где лежат .srs файлы (context.filesDir.absolutePath).
     *              Если null или файлы не найдены — умный роутинг RU отключается.
     */
    fun build(profile: ServerProfile, rulesDir: String? = null): String {
        val isServerIp = isIpAddress(profile.address)

        val geoipRuPath   = rulesDir?.let { "$it/$GEOIP_RU_FILENAME" }
        val geositeRuPath = rulesDir?.let { "$it/$GEOSITE_RU_FILENAME" }

        val hasGeoipRu   = geoipRuPath   != null && java.io.File(geoipRuPath).exists()
        val hasGeositeRu = geositeRuPath != null && java.io.File(geositeRuPath).exists()
        val hasRuRules   = hasGeoipRu || hasGeositeRu

        // ── DNS ───────────────────────────────────────────────────────────
        val dnsServers = buildList {
            add(mapOf(
                "tag"    to "dns-remote",
                "address" to "https://1.1.1.1/dns-query",
                "detour"  to "proxy",
            ))
            add(mapOf(
                "tag"     to "dns-direct",
                "address" to "8.8.8.8",
                "detour"  to "direct",
            ))
        }

        val dnsRules = buildList {
            // Домен VPN-сервера резолвим напрямую
            if (!isServerIp) {
                add(mapOf("domain" to listOf(profile.address), "server" to "dns-direct"))
            }
            // RU домены напрямую
            add(mapOf(
                "domain_suffix" to RU_DOMAINS,
                "server"        to "dns-direct",
            ))
            // RU-домены из rule-set
            if (hasGeositeRu) {
                add(mapOf(
                    "rule_set" to listOf("geosite-ru"),
                    "server"   to "dns-direct",
                ))
            }
        }

        // ── Route rules ───────────────────────────────────────────────────
        val routeRules = buildList {
            // DNS
            add(mapOf("protocol" to "dns", "outbound" to "dns-out"))
            // Приватные подсети напрямую
            add(mapOf("ip_cidr" to PRIVATE_CIDRS, "outbound" to "direct"))
            // RU домены напрямую
            add(mapOf(
                "domain_suffix" to RU_DOMAINS,
                "outbound"      to "direct",
            ))
            // Российские домены из rule-set
            if (hasGeositeRu) {
                add(mapOf(
                    "rule_set" to listOf("geosite-ru"),
                    "outbound" to "direct",
                ))
            }
            // Российские IP напрямую
            if (hasGeoipRu) {
                add(mapOf(
                    "rule_set" to listOf("geoip-ru"),
                    "outbound" to "direct",
                ))
            }
        }

        // ── Rule sets (локальные .srs файлы) ─────────────────────────────
        val ruleSets = buildList {
            if (hasGeositeRu) {
                add(mapOf(
                    "type"   to "local",
                    "tag"    to "geosite-ru",
                    "format" to "binary",
                    "path"   to geositeRuPath,
                ))
            }
            if (hasGeoipRu) {
                add(mapOf(
                    "type"   to "local",
                    "tag"    to "geoip-ru",
                    "format" to "binary",
                    "path"   to geoipRuPath,
                ))
            }
        }

        // ── Сборка конфига ────────────────────────────────────────────────
        val config = buildMap {
            put("log", mapOf("level" to "info", "timestamp" to true))

            put("dns", mapOf(
                "servers"  to dnsServers,
                "rules"    to dnsRules,
                "final"    to "dns-remote",
                "strategy" to "prefer_ipv4",
            ))

            put("inbounds", listOf(mapOf(
                "type"                       to "tun",
                "tag"                        to "tun-in",
                "inet4_address"              to TUN_ADDRESS_V4,
                "inet6_address"              to TUN_ADDRESS_V6,
                "mtu"                        to 9000,
                "auto_route"                 to false,
                "stack"                      to "gvisor",
                "sniff"                      to true,
                "sniff_override_destination" to false,
            )))

            put("outbounds", listOf(
                mapOf(
                    "type"        to "vless",
                    "tag"         to "proxy",
                    "server"      to profile.address,
                    "server_port" to profile.port,
                    "uuid"        to profile.uuid,
                    "flow"        to profile.flow.ifEmpty { null },
                    "tls" to mapOf(
                        "enabled"     to true,
                        "server_name" to profile.serverName,
                        "utls"        to mapOf(
                            "enabled"     to true,
                            "fingerprint" to profile.fingerprint.ifEmpty { "chrome" },
                        ),
                        "reality" to mapOf(
                            "enabled"    to true,
                            "public_key" to profile.publicKey,
                            "short_id"   to profile.shortId,
                        ),
                    ),
                ),
                mapOf("type" to "direct", "tag" to "direct"),
                mapOf("type" to "dns",    "tag" to "dns-out"),
            ))

            val routeMap = mutableMapOf(
                "final" to "proxy",
                "rules" to routeRules,
            )
            if (ruleSets.isNotEmpty()) {
                routeMap["rule_set"] = ruleSets
            }
            put("route", routeMap)
        }

        return gson.toJson(config)
    }

    private fun isIpAddress(host: String) =
        host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) || host.contains(':')

    private val PRIVATE_CIDRS = listOf(
        "10.0.0.0/8", "127.0.0.0/8", "169.254.0.0/16",
        "172.16.0.0/12", "192.168.0.0/16",
        "224.0.0.0/4", "240.0.0.0/4",
        "::1/128", "fc00::/7", "fe80::/10",
    )
}
