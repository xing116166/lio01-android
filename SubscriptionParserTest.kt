package com.xboard.app.vpn

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionParserTest {

    private fun b64(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray())

    private val vmessJson = """
        {"v":"2","ps":"香港 01","add":"hk.example.com","port":"443",
         "id":"11111111-1111-1111-1111-111111111111","aid":"0","scy":"auto",
         "net":"ws","type":"none","host":"hk.example.com","path":"/ws","tls":"tls",
         "sni":"hk.example.com"}
    """.trimIndent()

    @Test
    fun parsesBase64SubscriptionWithMultipleProtocols() {
        val links = listOf(
            "vmess://" + b64(vmessJson),
            "vless://22222222-2222-2222-2222-222222222222@jp.example.com:443?encryption=none&security=tls&sni=jp.example.com&type=ws&host=jp.example.com&path=%2Fws#JP01",
            "trojan://secret@sg.example.com:443?sni=sg.example.com#SG01",
            "ss://" + b64("aes-256-gcm:pass123") + "@us.example.com:8388#US01",
            "hysteria2://pass@hy.example.com:8443?sni=hy.example.com&insecure=1#HY01",
        ).joinToString("\n")

        val nodes = SubscriptionParser.parse(b64(links))

        assertEquals(5, nodes.size)
        assertEquals("vmess", nodes[0].protocol)
        assertEquals("香港 01", nodes[0].name)
        assertEquals("hk.example.com", nodes[0].server)
        assertEquals(443, nodes[0].port)

        val vmess = nodes[0].outbound.toString()
        assertTrue(vmess.contains("\"security\":\"auto\""))
        assertTrue(vmess.contains("\"path\":\"/ws\""))

        val vless = nodes[1].outbound.toString()
        assertTrue(vless.contains("\"uuid\":\"22222222-2222-2222-2222-222222222222\""))
        assertTrue(vless.contains("\"server_name\":\"jp.example.com\""))

        val trojan = nodes[2].outbound.toString()
        assertTrue(trojan.contains("\"password\":\"secret\""))

        val ss = nodes[3].outbound.toString()
        assertTrue(ss.contains("\"method\":\"aes-256-gcm\""))
        assertTrue(ss.contains("\"password\":\"pass123\""))

        val hy = nodes[4].outbound.toString()
        assertTrue(hy.contains("\"type\":\"hysteria2\""))
        assertTrue(hy.contains("\"insecure\":true"))
    }

    @Test
    fun plainLinkListIsAccepted() {
        val node = SubscriptionParser.parse("ss://" + b64("chacha20-ietf-poly1305:pw") + "@1.2.3.4:8388#节点")
        assertEquals(1, node.size)
        assertEquals("1.2.3.4", node[0].server)
        assertEquals(8388, node[0].port)
    }

    @Test
    fun clashYamlIsReportedAsUnsupported() {
        val yaml = """
            proxies:
              - name: node
                type: vmess
                server: example.com
        """.trimIndent()
        assertTrue(SubscriptionParser.parse(yaml).isEmpty())
    }

    @Test
    fun generatedConfigContainsTunAndDirectOutbound() {
        val node = SubscriptionParser.parse("ss://" + b64("aes-256-gcm:pw") + "@1.2.3.4:8388#n").first()
        val config = ProxyConfig.build(node)
        assertTrue(config.contains("\"type\":\"tun\""))
        assertTrue(config.contains("\"auto_route\":true"))
        assertTrue(config.contains("\"type\":\"direct\""))
        assertTrue(config.contains("\"final\":\"proxy\""))
        assertTrue(config.contains("\"server_port\":8388"))
    }
}
