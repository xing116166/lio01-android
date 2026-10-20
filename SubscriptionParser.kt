package com.xboard.app.vpn

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okio.ByteString.Companion.decodeBase64

data class VpnNode(
    val id: String,
    val name: String,
    val protocol: String,
    val server: String,
    val port: Int,
    val outbound: JsonObject,
)

/**
 * Parses the panel subscription into sing-box outbounds.
 *
 * Xboard returns a base64 encoded list of share links for non-Clash user
 * agents. VMess / VLESS / Trojan / Shadowsocks / Hysteria2 / TUIC are
 * supported; Clash YAML subscriptions are reported as unsupported.
 */
object SubscriptionParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): List<VpnNode> {
        val content = decodeSubscription(raw) ?: return emptyList()
        if (content.contains("proxies:") && content.contains("type:")) return emptyList()
        return content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains("://") }
            .mapNotNull { parseLink(it) }
            .distinctBy { it.server + ":" + it.port + ":" + it.name }
            .toList()
    }

    private fun decodeSubscription(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (text.contains("://")) return text
        return decodeBase64Flexible(text)
    }

    private fun parseLink(link: String): VpnNode? = runCatching {
        when {
            link.startsWith("vmess://", true) -> parseVmess(link)
            link.startsWith("vless://", true) -> parseVless(link)
            link.startsWith("trojan://", true) -> parseTrojan(link)
            link.startsWith("ss://", true) -> parseShadowsocks(link)
            link.startsWith("hysteria2://", true) || link.startsWith("hy2://", true) -> parseHysteria2(link)
            link.startsWith("tuic://", true) -> parseTuic(link)
            else -> null
        }
    }.getOrNull()

    // ---------------------------------------------------------------- vmess

    private fun parseVmess(link: String): VpnNode? {
        val payload = link.removePrefix("vmess://").substringBefore('#')
        val decoded = decodeBase64Flexible(payload) ?: payload
        val obj = json.parseToJsonElement(decoded).jsonObject
        val server = obj.str("add") ?: return null
        val port = obj.int("port") ?: return null
        val uuid = obj.str("id") ?: return null
        val name = obj.str("ps") ?: "$server:$port"
        val net = obj.str("net")?.lowercase() ?: "tcp"
        val host = obj.str("host").orEmpty()
        val path = obj.str("path").orEmpty()
        val tlsEnabled = obj.str("tls")?.equals("tls", true) == true
        val security = obj.str("scy") ?: "auto"

        val outbound = buildJsonObject {
            put("type", "vmess")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("uuid", uuid)
            put("security", security)
            put("alter_id", obj.int("aid") ?: 0)
            if (tlsEnabled) {
                put("tls", buildTls(obj.str("sni") ?: host, alpn = obj.str("alpn")))
            }
            buildTransport(net, host, path)?.let { put("transport", it) }
        }
        return node("vmess", name, server, port, outbound)
    }

    // ---------------------------------------------------------------- vless

    private fun parseVless(link: String): VpnNode? {
        val uri = URI(link)
        val uuid = uri.userInfo ?: return null
        val server = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else 443
        val params = query(uri)
        val security = params["security"]?.lowercase() ?: "none"
        val tlsEnabled = security == "tls" || security == "reality" || security == "xtls"
        val name = fragmentName(uri) ?: "$server:$port"
        val sni = params["sni"] ?: params["host"] ?: server
        val transport = buildTransport(
            params["type"] ?: "tcp",
            params["host"].orEmpty(),
            params["path"].orEmpty(),
            params["serviceName"],
        )
        val outbound = buildJsonObject {
            put("type", "vless")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("uuid", uuid)
            params["flow"]?.takeIf { it.isNotBlank() }?.let { put("flow", it) }
            if (tlsEnabled) {
                val reality = if (security == "reality") {
                    params["pbk"]?.let { publicKey -> publicKey to params["sid"].orEmpty() }
                } else {
                    null
                }
                put("tls", buildTls(sni, params["allowInsecure"] == "1", params["alpn"], reality))
            }
            transport?.let { put("transport", it) }
        }
        return node("vless", name, server, port, outbound)
    }

    // --------------------------------------------------------------- trojan

    private fun parseTrojan(link: String): VpnNode? {
        val uri = URI(link)
        val password = uri.userInfo?.let { URLDecoder.decode(it, "UTF-8") } ?: return null
        val server = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else 443
        val params = query(uri)
        val name = fragmentName(uri) ?: "$server:$port"
        val transport = buildTransport(
            params["type"] ?: "tcp",
            params["host"].orEmpty(),
            params["path"].orEmpty(),
            params["serviceName"],
        )
        val outbound = buildJsonObject {
            put("type", "trojan")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("password", password)
            put("tls", buildTls(params["sni"] ?: server, params["allowInsecure"] == "1", params["alpn"]))
            transport?.let { put("transport", it) }
        }
        return node("trojan", name, server, port, outbound)
    }

    // ----------------------------------------------------------- shadowsocks

    private fun parseShadowsocks(link: String): VpnNode? {
        var body = link.removePrefix("ss://")
        val name = body.substringAfter('#', "").takeIf { it.isNotBlank() }?.let { URLDecoder.decode(it, "UTF-8") }
        body = body.substringBefore('#')
        val queryIndex = body.indexOf('?')
        if (queryIndex >= 0) body = body.substring(0, queryIndex)

        var userInfo: String?
        var hostPort: String
        if (body.contains('@')) {
            val at = body.lastIndexOf('@')
            userInfo = decodeBase64Flexible(body.substring(0, at)) ?: body.substring(0, at)
            hostPort = body.substring(at + 1)
        } else {
            val decoded = decodeBase64Flexible(body) ?: return null
            val at = decoded.lastIndexOf('@')
            if (at < 0) return null
            userInfo = decoded.substring(0, at)
            hostPort = decoded.substring(at + 1)
        }

        val separator = userInfo.indexOf(':')
        if (separator <= 0) return null
        val method = userInfo.substring(0, separator)
        val password = userInfo.substring(separator + 1)
        val server = hostPort.substringBeforeLast(':')
        val port = hostPort.substringAfterLast(':').toIntOrNull() ?: return null
        if (server.isBlank()) return null

        val outbound = buildJsonObject {
            put("type", "shadowsocks")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("method", method)
            put("password", password)
        }
        return node("ss", name ?: "$server:$port", server, port, outbound)
    }

    // ------------------------------------------------------------ hysteria2

    private fun parseHysteria2(link: String): VpnNode? {
        val uri = URI(link)
        val password = uri.userInfo?.let { URLDecoder.decode(it, "UTF-8") } ?: return null
        val server = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else 443
        val params = query(uri)
        val name = fragmentName(uri) ?: "$server:$port"
        val outbound = buildJsonObject {
            put("type", "hysteria2")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("password", password)
            put(
                "tls",
                buildTls(
                    params["sni"] ?: server,
                    params["insecure"] == "1" || params["allowInsecure"] == "1",
                ),
            )
            if (params["obfs"] == "salamander" && !params["obfs-password"].isNullOrBlank()) {
                put(
                    "obfs",
                    buildJsonObject {
                        put("type", "salamander")
                        put("password", params["obfs-password"])
                    },
                )
            }
        }
        return node("hysteria2", name, server, port, outbound)
    }

    // ----------------------------------------------------------------- tuic

    private fun parseTuic(link: String): VpnNode? {
        val uri = URI(link)
        val userInfo = uri.userInfo ?: return null
        val separator = userInfo.indexOf(':')
        if (separator <= 0) return null
        val uuid = userInfo.substring(0, separator)
        val password = URLDecoder.decode(userInfo.substring(separator + 1), "UTF-8")
        val server = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else 443
        val params = query(uri)
        val name = fragmentName(uri) ?: "$server:$port"
        val outbound = buildJsonObject {
            put("type", "tuic")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("uuid", uuid)
            put("password", password)
            put(
                "tls",
                buildTls(
                    params["sni"] ?: server,
                    params["allowInsecure"] == "1" || params["insecure"] == "1",
                    params["alpn"],
                ),
            )
        }
        return node("tuic", name, server, port, outbound)
    }

    // -------------------------------------------------------------- helpers

    private fun node(type: String, name: String, server: String, port: Int, outbound: JsonObject): VpnNode {
        val id = UUID.nameUUIDFromBytes("$type|$name|$server|$port".toByteArray()).toString()
        return VpnNode(id, name, type, server, port, outbound)
    }

    private fun buildTls(
        serverName: String,
        insecure: Boolean = false,
        alpn: String? = null,
        reality: Pair<String, String>? = null,
    ): JsonObject = buildJsonObject {
        put("enabled", true)
        if (serverName.isNotBlank()) put("server_name", serverName)
        if (insecure) put("insecure", true)
        if (!alpn.isNullOrBlank()) {
            put("alpn", buildJsonArray { alpn.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) } })
        }
        if (reality != null) {
            put(
                "reality",
                buildJsonObject {
                    put("enabled", true)
                    put("public_key", reality.first)
                    if (reality.second.isNotBlank()) put("short_id", reality.second)
                },
            )
        }
    }

    private fun buildTransport(net: String, host: String, path: String, serviceName: String? = null): JsonObject? =
        when (net.lowercase()) {
            "ws" -> buildJsonObject {
                put("type", "ws")
                if (path.isNotBlank()) put("path", path)
                if (host.isNotBlank()) {
                    put("headers", buildJsonObject { put("Host", host) })
                }
            }
            "grpc" -> buildJsonObject {
                put("type", "grpc")
                put("service_name", serviceName ?: path)
            }
            "h2", "http" -> buildJsonObject {
                put("type", "http")
                if (host.isNotBlank()) put("host", buildJsonArray { add(host) })
                if (path.isNotBlank()) put("path", path)
            }
            "quic" -> buildJsonObject { put("type", "quic") }
            else -> null
        }

    private fun query(uri: URI): Map<String, String> {
        val raw = uri.rawQuery ?: return emptyMap()
        return raw.split('&').mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val index = part.indexOf('=')
            val key = if (index >= 0) part.substring(0, index) else part
            val value = if (index >= 0) part.substring(index + 1) else ""
            decode(key) to decode(value)
        }.toMap()
    }

    private fun fragmentName(uri: URI): String? =
        uri.rawFragment?.takeIf { it.isNotBlank() }?.let { decode(it) }

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun decodeBase64Flexible(value: String): String? {
        val cleaned = value.trim().replace("\r", "").replace("\n", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        var normalized = cleaned.replace('-', '+').replace('_', '/')
        while (normalized.length % 4 != 0) normalized += "="
        return runCatching { normalized.decodeBase64()?.utf8() }.getOrNull()
    }

    private fun JsonObject.str(key: String): String? = runCatching {
        this[key]?.jsonPrimitive?.contentOrNull
    }.getOrNull()?.takeIf { it.isNotBlank() && it != "null" }

    private fun JsonObject.int(key: String): Int? = runCatching {
        this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    }.getOrNull()

    fun JsonObject.toJsonString(): String = this.toString()

    fun elementToString(element: JsonElement?): String = element?.toString().orEmpty()

    fun JsonArray.sizeOrZero(): Int = this.size
}
