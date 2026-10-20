package com.xboard.app.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class XboardApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: XboardApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = XboardApi()
        api.baseUrl = server.url("/").toString().trimEnd('/')
        api.authHeader = "Bearer test-token"
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueue(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    @Test
    fun loginParsesAuthDataAndSendsJsonBody() = runBlocking {
        enqueue(
            """{"status":"success","message":"","data":{"token":"sub-token","auth_data":"Bearer abc123","is_admin":false}}""",
        )

        val auth = api.login("user@example.com", "password123")

        assertEquals("Bearer abc123", auth.auth_data)
        assertEquals("sub-token", auth.token)
        assertEquals(false, auth.is_admin)

        val request = server.takeRequest()
        assertEquals("/api/v1/passport/auth/login", request.path)
        assertEquals("POST", request.method)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"email\":\"user@example.com\""))
        assertTrue(body.contains("\"password\":\"password123\""))
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
    }

    @Test
    fun userInfoToleratesNullFields() = runBlocking {
        enqueue(
            """{"status":"success","data":{"email":"a@b.com","transfer_enable":null,"balance":1234,"expired_at":1700000000,"uuid":null}}""",
        )

        val user = api.userInfo()

        assertEquals("a@b.com", user.email)
        assertNull(user.transfer_enable)
        assertEquals(1234L, user.balance)
        assertEquals(1700000000L, user.expired_at)
    }

    @Test
    fun plansParsePricesAndNullableCapacity() = runBlocking {
        enqueue(
            """{"status":"success","data":[
                {"id":1,"name":"Pro","month_price":1000,"year_price":null,"tags":["热门"],"transfer_enable":1048576,"capacity_limit":"Sold out"},
                {"id":2,"name":"Lite","month_price":500,"capacity_limit":10}
            ]}""",
        )

        val plans = api.plans()

        assertEquals(2, plans.size)
        assertEquals(1000.0, plans[0].month_price!!, 0.001)
        assertNull(plans[0].year_price)
        assertEquals(listOf("热门"), plans[0].tags)
        assertTrue(plans[1].capacity_limit.toString().contains("10"))
    }

    @Test
    fun createOrderSendsLegacyPeriodAndReturnsTradeNo() = runBlocking {
        enqueue("""{"status":"success","data":"2024111100001"}""")

        val tradeNo = api.createOrder(planId = 7, period = "month_price", couponCode = "SAVE10")

        assertEquals("2024111100001", tradeNo)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"plan_id\":7"))
        assertTrue(body.contains("\"period\":\"month_price\""))
        assertTrue(body.contains("\"coupon_code\":\"SAVE10\""))
    }

    @Test
    fun checkoutMapsPluginResultTypes() = runBlocking {
        enqueue("""{"type":0,"data":"https://pay.example.com/redirect"}""")
        val redirect = api.checkout("T1", 2)
        assertTrue(redirect is PaymentAction.OpenUrl)
        assertEquals("https://pay.example.com/redirect", (redirect as PaymentAction.OpenUrl).url)

        enqueue("""{"type":0,"data":"<form action=\"pay\"></form>"}""")
        val raw = api.checkout("T1", 2)
        assertTrue(raw is PaymentAction.Raw)

        enqueue("""{"type":1,"data":"https://pay.example.com/qr"}""")
        val qr = api.checkout("T1", 2)
        assertTrue(qr is PaymentAction.QrCode)
        assertEquals("https://pay.example.com/qr", (qr as PaymentAction.QrCode).content)

        enqueue("""{"type":-1,"data":true}""")
        assertEquals(PaymentAction.Completed, api.checkout("T1", null))
    }

    @Test
    fun orderStatusIsParsedFromPrimitiveData() = runBlocking {
        enqueue("""{"status":"success","data":3}""")
        assertEquals(3, api.checkOrder("T1"))
    }

    @Test
    fun noticesAreTopLevelPayload() = runBlocking {
        enqueue("""{"data":[{"id":1,"title":"维护通知","content":"<p>今晚维护</p>"}],"total":1}""")

        val page = api.notices()

        assertEquals(1, page.total)
        assertEquals("维护通知", page.data.first().title)
    }

    @Test
    fun businessErrorExposesServerMessage() = runBlocking {
        enqueue("""{"status":"fail","message":"余额不足","data":null}""", code = 400)

        try {
            api.createOrder(1, "month_price", null)
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals("余额不足", e.message)
            assertEquals(400, e.statusCode)
        }
    }

    @Test
    fun unauthenticatedResponseBecomesUnauthorizedException() = runBlocking {
        enqueue("""{"status":"fail","message":"未登录或登陆已过期","data":null}""", code = 403)

        try {
            api.userInfo()
            fail("expected UnauthorizedException")
        } catch (e: UnauthorizedException) {
            assertTrue(e.message!!.contains("登录"))
        }
    }

    @Test
    fun networkFailureIsWrapped() = runBlocking {
        server.shutdown()
        try {
            api.guestConfig()
            fail("expected NetworkException")
        } catch (e: NetworkException) {
            assertTrue(e.message!!.contains("网络"))
        }
    }
}
