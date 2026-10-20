package com.xboard.app.util

import com.xboard.app.data.Plan
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val decimal = DecimalFormat("0.##")

/** User traffic (`transfer_enable`, `u`, `d`) from Xboard is stored in bytes. */
fun formatTraffic(bytes: Long?): String {
    val value = bytes ?: 0L
    if (value <= 0) return "0 MB"
    val gb = value / 1024.0 / 1024.0 / 1024.0
    return when {
        value < 1024 -> "$value B"
        value < 1024L * 1024 -> "${decimal.format(value / 1024.0)} KB"
        value < 1024L * 1024 * 1024 -> "${decimal.format(value / 1024.0 / 1024.0)} MB"
        gb < 1024 -> "${decimal.format(gb)} GB"
        else -> "${decimal.format(gb / 1024.0)} TB"
    }
}

/** Plan traffic is configured in GB by the panel and converted to bytes on purchase. */
fun formatPlanTraffic(gb: Long?): String {
    val value = gb ?: 0L
    if (value <= 0) return "不限"
    return formatTraffic(value * 1024L * 1024L * 1024L)
}

/** Monetary amounts are stored in cents by the panel. */
fun formatMoney(cents: Long?, symbol: String = "¥"): String {
    val value = (cents ?: 0L) / 100.0
    return "$symbol${decimal.format(value)}"
}

fun formatPrice(cents: Double?, symbol: String = "¥"): String {
    if (cents == null || cents <= 0) return "$symbol--"
    return "$symbol${decimal.format(cents / 100.0)}"
}

fun formatDate(timestamp: Long?, withTime: Boolean = false): String {
    if (timestamp == null || timestamp <= 0) return "--"
    val pattern = if (withTime) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp * 1000))
}

fun daysLeft(expiredAt: Long?): Long {
    if (expiredAt == null || expiredAt <= 0) return -1
    val diff = expiredAt * 1000 - System.currentTimeMillis()
    if (diff <= 0) return 0
    return diff / (24 * 60 * 60 * 1000)
}

fun expireLabel(expiredAt: Long?): String {
    if (expiredAt == null || expiredAt <= 0) return "长期有效"
    val days = daysLeft(expiredAt)
    return if (days <= 0) "已过期" else "剩余 $days 天"
}

fun normalizeBaseUrl(raw: String): String {
    var value = raw.trim()
    if (value.isEmpty()) return ""
    value = value.substringBefore('#').substringBefore('?')
    if (!value.startsWith("http://", ignoreCase = true) &&
        !value.startsWith("https://", ignoreCase = true)
    ) {
        value = "https://$value"
    }
    value = value.trimEnd('/')
    if (value.endsWith("/api/v1")) value = value.removeSuffix("/api/v1")
    return value.trimEnd('/')
}

fun isValidBaseUrl(url: String): Boolean {
    val rest = url.removePrefix("https://").removePrefix("http://")
    if (rest.isBlank()) return false
    val host = rest.substringBefore('/').substringBefore(':')
    return host.isNotBlank() && (host.contains('.') || host == "localhost" || host.matches(Regex("\\d+(\\.\\d+){3}")))
}

fun stripHtml(html: String?): String {
    if (html.isNullOrBlank()) return ""
    var text = html
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
    text = text.replace(Regex("\\n{3,}"), "\n\n")
    return text.trim()
}

data class PeriodOption(
    val key: String,
    val label: String,
    val priceCents: Double?,
)

fun periodLabel(legacyKey: String): String = when (legacyKey) {
    "month_price" -> "月付"
    "quarter_price" -> "季付"
    "half_year_price" -> "半年付"
    "year_price" -> "年付"
    "two_year_price" -> "两年付"
    "three_year_price" -> "三年付"
    "onetime_price" -> "一次性"
    "reset_price" -> "流量重置"
    else -> legacyKey
}

fun periodDays(legacyKey: String): Int = when (legacyKey) {
    "month_price" -> 30
    "quarter_price" -> 90
    "half_year_price" -> 180
    "year_price" -> 365
    "two_year_price" -> 730
    "three_year_price" -> 1095
    else -> -1
}

fun Plan.availablePeriods(): List<PeriodOption> {
    val options = listOf(
        PeriodOption("month_price", "月付", month_price),
        PeriodOption("quarter_price", "季付", quarter_price),
        PeriodOption("half_year_price", "半年付", half_year_price),
        PeriodOption("year_price", "年付", year_price),
        PeriodOption("two_year_price", "两年付", two_year_price),
        PeriodOption("three_year_price", "三年付", three_year_price),
        PeriodOption("onetime_price", "一次性", onetime_price),
        PeriodOption("reset_price", "流量重置", reset_price),
    )
    return options.filter { (it.priceCents ?: 0.0) > 0.0 }
}

fun Plan.lowestPrice(): String {
    val cheapest = availablePeriods().minByOrNull { it.priceCents ?: Double.MAX_VALUE }
    return if (cheapest == null) "暂未定价" else "¥${decimal.format((cheapest.priceCents ?: 0.0) / 100.0)} 起"
}

fun orderStatusText(status: Int): String = when (status) {
    0 -> "待支付"
    1 -> "开通中"
    2 -> "已取消"
    3 -> "已完成"
    4 -> "已折抵"
    else -> "未知"
}

fun orderTypeText(type: Int): String = when (type) {
    1 -> "新购"
    2 -> "续费"
    3 -> "升级"
    4 -> "流量重置"
    else -> "订单"
}

fun nodeTypeLabel(type: String): String = when (type.lowercase()) {
    "shadowsocks" -> "SS"
    "shadowsocks2022" -> "SS2022"
    "vmess" -> "VMess"
    "vless" -> "VLESS"
    "trojan" -> "Trojan"
    "tuic" -> "Tuic"
    "hysteria" -> "Hysteria"
    "hysteria2" -> "Hysteria2"
    "anytls" -> "AnyTLS"
    else -> type.uppercase(Locale.ROOT).ifBlank { "节点" }
}

fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 0) return email
    val name = email.substring(0, at)
    val domain = email.substring(at)
    val shown = if (name.length <= 2) name.take(1) + "*" else name.take(2) + "***"
    return shown + domain
}
