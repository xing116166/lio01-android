package com.xboard.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Xboard (V2Board compatible) API models.
 *
 * The panel returns a JSON envelope for most endpoints:
 *   { "status": "success", "message": "...", "data": ... }
 * A few legacy endpoints (notices, checkout, invite details) return the payload
 * at the top level, which is handled separately in [XboardApi].
 */
@Serializable
data class ApiEnvelope<T>(
    val status: String? = null,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class PanelConfig(
    val app_description: String? = null,
    val app_url: String? = null,
    val logo: String? = null,
    val tos_url: String? = null,
    val is_email_verify: Int = 0,
    val is_invite_force: Int = 0,
    val is_captcha: Int = 0,
    val captcha_type: String? = null,
    val recaptcha_site_key: String? = null,
    val turnstile_site_key: String? = null,
    @SerialName("is_recaptcha") val is_recaptcha: Int = 0,
)

@Serializable
data class UserConfig(
    val is_telegram: Int = 0,
    val telegram_discuss_link: String? = null,
    val currency: String? = null,
    val currency_symbol: String? = null,
    val withdraw_close: Int = 0,
)

@Serializable
data class AuthData(
    val token: String = "",
    val auth_data: String = "",
    val is_admin: Boolean = false,
)

@Serializable
data class UserInfo(
    val email: String = "",
    val transfer_enable: Long? = null,
    val last_login_at: Long? = null,
    val created_at: Long? = null,
    val banned: Boolean? = null,
    val remind_expire: Boolean? = null,
    val remind_traffic: Boolean? = null,
    val expired_at: Long? = null,
    val balance: Long? = null,
    val commission_balance: Long? = null,
    val plan_id: Int? = null,
    val discount: Int? = null,
    val commission_rate: Double? = null,
    val telegram_id: Long? = null,
    val uuid: String? = null,
    val avatar_url: String? = null,
)

@Serializable
data class Subscribe(
    val plan_id: Int? = null,
    val token: String = "",
    val expired_at: Long? = null,
    val u: Long? = null,
    val d: Long? = null,
    val transfer_enable: Long? = null,
    val email: String = "",
    val uuid: String? = null,
    val device_limit: Int? = null,
    val speed_limit: Int? = null,
    val next_reset_at: Long? = null,
    val plan: Plan? = null,
    val subscribe_url: String = "",
    val reset_day: Int? = null,
)

@Serializable
data class Plan(
    val id: Int = 0,
    val group_id: Int? = null,
    val name: String = "",
    val tags: List<String>? = null,
    val content: String? = null,
    @SerialName("month_price") val month_price: Double? = null,
    @SerialName("quarter_price") val quarter_price: Double? = null,
    @SerialName("half_year_price") val half_year_price: Double? = null,
    @SerialName("year_price") val year_price: Double? = null,
    @SerialName("two_year_price") val two_year_price: Double? = null,
    @SerialName("three_year_price") val three_year_price: Double? = null,
    @SerialName("onetime_price") val onetime_price: Double? = null,
    @SerialName("reset_price") val reset_price: Double? = null,
    val capacity_limit: JsonElement? = null,
    val transfer_enable: Long? = null,
    val speed_limit: Int? = null,
    val device_limit: Int? = null,
    val show: Boolean? = null,
    val sell: Boolean? = null,
    val renew: Boolean? = null,
    @SerialName("reset_traffic_method") val reset_traffic_method: Int? = null,
    val sort: Int? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null,
)

@Serializable
data class Order(
    val id: Int = 0,
    val user_id: Int? = null,
    val plan_id: Int? = null,
    val payment_id: Int? = null,
    val period: String = "",
    val trade_no: String = "",
    val total_amount: Long = 0,
    val handling_amount: Long? = null,
    val balance_amount: Long? = null,
    val surplus_credit: Long? = null,
    val surplus_amount: Long? = null,
    val type: Int = 1,
    val status: Int = 0,
    val coupon_id: Int? = null,
    val discount_amount: Long? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null,
    val paid_at: Long? = null,
    val plan: Plan? = null,
    val payment: PaymentInfo? = null,
)

@Serializable
data class PaymentInfo(
    val id: Int = 0,
    val name: String = "",
    val payment: String = "",
    val icon: String? = null,
)

@Serializable
data class PaymentMethod(
    val id: Int = 0,
    val name: String = "",
    val payment: String = "",
    val icon: String? = null,
    val handling_fee_fixed: Long? = null,
    val handling_fee_percent: Double? = null,
)

@Serializable
data class Node(
    val id: Int = 0,
    val type: String = "",
    val version: String? = null,
    val name: String = "",
    val rate: Double? = null,
    val tags: List<String>? = null,
    @SerialName("is_online") val is_online: Boolean? = null,
    @SerialName("cache_key") val cache_key: String? = null,
    @SerialName("last_check_at") val last_check_at: Long? = null,
)

@Serializable
data class Notice(
    val id: Int = 0,
    val title: String = "",
    val content: String? = null,
    val show: Boolean? = null,
    val sort: Int? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null,
)

@Serializable
data class NoticePage(
    val data: List<Notice> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class InviteCode(
    val id: Int = 0,
    val code: String = "",
    val status: Int = 0,
    val created_at: Long? = null,
    val updated_at: Long? = null,
)

@Serializable
data class InviteInfo(
    val codes: List<InviteCode> = emptyList(),
    /** [registered users, valid commission, pending commission, rate %, available balance] */
    val stat: List<Long> = emptyList(),
)

@Serializable
data class CheckoutResult(
    val type: Int = 0,
    val data: JsonElement? = null,
)

@Serializable
data class TrafficRecord(
    val u: Long? = null,
    val d: Long? = null,
    @SerialName("record_at") val record_at: Long? = null,
)

/** A payment flow step returned by /user/order/checkout. */
sealed interface PaymentAction {
    /** type = -1: nothing to pay (free / fully discounted / balance covered). */
    data object Completed : PaymentAction

    /** type = 0: open a payment URL in the browser. */
    data class OpenUrl(val url: String) : PaymentAction

    /** type = 1: render the payload as a QR code. */
    data class QrCode(val content: String) : PaymentAction

    /** Any other plugin-specific payload: show it as text. */
    data class Raw(val content: String) : PaymentAction

    data object Unknown : PaymentAction
}
