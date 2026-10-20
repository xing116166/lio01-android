package com.xboard.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xboard.app.data.Session
import com.xboard.app.data.SessionStore
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the main screens against a mocked Xboard panel and stores PNGs under
 * app/build/screenshots for visual verification.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var server: MockWebServer

    private val info = """
        {"status":"success","data":{"email":"demo@xboard.dev","transfer_enable":107374182400,
        "expired_at":1900000000,"balance":8888,"commission_balance":1200,"plan_id":1,
        "uuid":"0000","banned":false,"created_at":1700000000,"discount":0,"commission_rate":10}}
    """.trimIndent()

    private val subscribe = """
        {"status":"success","data":{"plan_id":1,"token":"sub-token","expired_at":1900000000,
        "u":12884901888,"d":23622320128,"transfer_enable":107374182400,"device_limit":5,
        "speed_limit":null,"next_reset_at":1905000000,
        "subscribe_url":"https://demo.xboard.dev/api/v1/client/subscribe?token=abcdef123456",
        "plan":{"id":1,"name":"旗舰套餐","tags":["高速","流媒体"],"content":"<p>解锁 Netflix / ChatGPT，全球节点任选</p>",
        "transfer_enable":100,"speed_limit":null,"device_limit":5,"month_price":2999,"year_price":29999},
        "reset_day":1}}
    """.trimIndent()

    private val plans = """
        {"status":"success","data":[
        {"id":1,"name":"旗舰套餐","tags":["高速","流媒体"],"content":"<p>解锁 Netflix / ChatGPT，全球节点任选</p>",
         "month_price":2999,"quarter_price":8499,"year_price":29999,"transfer_enable":100,
         "speed_limit":null,"device_limit":5,"capacity_limit":100,"sell":true},
        {"id":2,"name":"轻量套餐","tags":["入门"],"content":"<p>适合日常轻量使用</p>",
         "month_price":999,"year_price":9999,"transfer_enable":20,
         "speed_limit":200,"device_limit":3,"capacity_limit":null,"sell":true}
        ]}
    """.trimIndent()

    private val nodes = """
        {"status":"success","data":[
        {"id":1,"type":"vmess","name":"香港 01 · IPLC","rate":1,"tags":["HK","流媒体"],"is_online":true,"cache_key":"a","last_check_at":1900000000},
        {"id":2,"type":"vless","name":"日本 02 · 软银","rate":1.5,"tags":["JP"],"is_online":true,"cache_key":"b","last_check_at":1900000000},
        {"id":3,"type":"trojan","name":"新加坡 01","rate":1,"tags":["SG"],"is_online":true,"cache_key":"c","last_check_at":1900000000},
        {"id":4,"type":"hysteria2","name":"美国 03 · CN2 GIA","rate":2,"tags":["US","游戏"],"is_online":false,"cache_key":"d","last_check_at":1900000000}
        ]}
    """.trimIndent()

    private val notices = """
        {"data":[{"id":1,"title":"国庆假期节点维护通知","content":"<p>10 月 1 日 02:00-04:00 将对香港节点进行升级，期间可能短暂中断。</p>","created_at":1890000000}],"total":1}
    """.trimIndent()

    private fun b64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray())

    private fun sampleSubscription(): String {
        val vmess = b64(
            """{"v":"2","ps":"香港 01 · IPLC","add":"hk.example.com","port":"443","id":"11111111-1111-1111-1111-111111111111","aid":"0","scy":"auto","net":"ws","host":"hk.example.com","path":"/ws","tls":"tls","sni":"hk.example.com"}""",
        )
        val links = listOf(
            "vmess://$vmess",
            "vless://22222222-2222-2222-2222-222222222222@jp.example.com:443?encryption=none&security=tls&sni=jp.example.com&type=ws&host=jp.example.com&path=%2Fws#%E6%97%A5%E6%9C%AC%2002",
            "trojan://secret@sg.example.com:443?sni=sg.example.com#%E6%96%B0%E5%8A%A0%E5%9D%A1%2001",
        ).joinToString("\n")
        return b64(links)
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                val body = when {
                    path.contains("/guest/comm/config") ->
                        """{"status":"success","data":{"app_description":"lio01 面板","app_url":"https://demo.xboard.dev","is_email_verify":0,"is_invite_force":0,"is_captcha":0,"is_recaptcha":0}}"""
                    path.contains("/user/comm/config") ->
                        """{"status":"success","data":{"currency":"CNY","currency_symbol":"¥","is_telegram":0}}"""
                    path.contains("/user/info") -> info
                    path.contains("/user/getSubscribe") ->
                        subscribe.replace("https://demo.xboard.dev", server.url("/").toString().trimEnd('/'))
                    path.contains("/sub") -> sampleSubscription()
                    path.contains("/user/plan/fetch") -> plans
                    path.contains("/user/server/fetch") -> nodes
                    path.contains("/user/order/fetch") -> """{"status":"success","data":[]}"""
                    path.contains("/user/notice/fetch") -> notices
                    path.contains("/user/order/getPaymentMethod") -> """{"status":"success","data":[]}"""
                    else -> """{"status":"success","data":null}"""
                }
                return MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(body)
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun seed(token: String, email: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            SessionStore(context).save(
                Session(
                    baseUrl = server.url("/").toString().trimEnd('/'),
                    token = token,
                    email = email,
                    theme = "light",
                ),
            )
        }
    }

    private fun capture(scenario: ActivityScenario<MainActivity>, name: String) {
        compose.waitForIdle()
        var bitmap: Bitmap? = null
        scenario.onActivity { activity ->
            val view = activity.window.decorView
            if (view.width > 0 && view.height > 0) {
                val rendered = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(rendered))
                bitmap = rendered
            }
        }
        val rendered = bitmap ?: error("decor view has no size")
        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { out ->
            rendered.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Test
    fun captureLogin() {
        seed(token = "", email = "demo@xboard.dev")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithText("欢迎回来").fetchSemanticsNodes().isNotEmpty()
            }
            capture(scenario, "login")
        }
    }

    @Test
    fun captureMainScreens() {
        seed(token = "Bearer demo-token", email = "demo@xboard.dev")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithText("你好，demo").fetchSemanticsNodes().isNotEmpty()
            }
            capture(scenario, "home")

            compose.onNodeWithText("套餐").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("轻量套餐").fetchSemanticsNodes().isNotEmpty()
            }
            capture(scenario, "plans")

            compose.onNodeWithText("旗舰套餐").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("选择周期").fetchSemanticsNodes().isNotEmpty()
            }
            capture(scenario, "plan-detail")

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()

            compose.onNodeWithText("节点").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("香港 01 · IPLC").fetchSemanticsNodes().isNotEmpty()
            }
            capture(scenario, "nodes")

            compose.onNodeWithText("我的").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("demo@xboard.dev").fetchSemanticsNodes().isNotEmpty()
            }
            capture(scenario, "profile")
        }
    }
}
