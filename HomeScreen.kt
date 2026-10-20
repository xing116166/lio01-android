package com.xboard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xboard.app.AppState
import com.xboard.app.data.Notice
import com.xboard.app.ui.components.EmptyState
import com.xboard.app.ui.components.SectionTitle
import com.xboard.app.ui.components.SurfaceCard
import com.xboard.app.ui.components.TagPill
import com.xboard.app.ui.theme.HeroGradient
import com.xboard.app.util.expireLabel
import com.xboard.app.util.formatDate
import com.xboard.app.util.formatMoney
import com.xboard.app.util.formatTraffic
import com.xboard.app.util.stripHtml

@Composable
fun HomeScreen(
    state: AppState,
    onBuy: () -> Unit,
    onOrders: () -> Unit,
    onNodes: () -> Unit,
    onInvite: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenNotice: (Notice) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val sub = state.subscribe
    val used = (sub?.u ?: 0L) + (sub?.d ?: 0L)
    val total = sub?.transfer_enable ?: 0L
    val fraction = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val email = state.user?.email ?: state.session.email
    val symbol = state.currencySymbol

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(HeroGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = email.take(1).uppercase().ifBlank { "X" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "你好，${email.substringBefore('@').ifBlank { "旅行者" }}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.config?.app_description?.takeIf { it.isNotBlank() } ?: state.session.host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRefresh) {
                if (state.refreshing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "设置")
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- hero subscription card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(HeroGradient))
                .padding(22.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.isActive) (sub?.plan?.name ?: "当前订阅") else "尚未订阅",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (state.isActive) "生效中" else "未开通",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (total > 0) formatTraffic((total - used).coerceAtLeast(0)) else "--",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = if (total > 0) "剩余流量 / 共 ${formatTraffic(total)}" else "购买套餐后开始使用",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                )

                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "已用 ${formatTraffic(used)} · ${(fraction * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                )

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroStat(
                        label = "到期时间",
                        value = formatDate(sub?.expired_at),
                        modifier = Modifier.weight(1f),
                    )
                    HeroStat(
                        label = "订阅状态",
                        value = expireLabel(sub?.expired_at),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroButton(
                        text = if (state.isActive) "续费订阅" else "选购套餐",
                        modifier = Modifier.weight(1f),
                        onClick = onBuy,
                    )
                    HeroButton(
                        text = "复制订阅",
                        icon = Icons.Rounded.ContentCopy,
                        modifier = Modifier.weight(1f),
                        filled = false,
                        onClick = {
                            val url = sub?.subscribe_url
                            if (!url.isNullOrBlank()) clipboard.setText(AnnotatedString(url))
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- quick stats
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "账户余额",
                value = formatMoney(state.user?.balance ?: 0, symbol),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "设备限制",
                value = sub?.device_limit?.takeIf { it > 0 }?.let { "$it 台" } ?: "不限",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "下次重置",
                value = sub?.next_reset_at?.takeIf { it > 0 }?.let { formatDate(it) } ?: "不重置",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("快捷入口")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("购买套餐", Icons.Rounded.ShoppingBag, Modifier.weight(1f), onBuy)
            QuickAction("我的订单", Icons.Rounded.Receipt, Modifier.weight(1f), onOrders)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("订阅节点", Icons.Rounded.Speed, Modifier.weight(1f), onNodes)
            QuickAction("邀请好友", Icons.Rounded.Group, Modifier.weight(1f), onInvite)
        }

        // ---- announcements
        Spacer(Modifier.height(24.dp))
        SectionTitle("公告")
        Spacer(Modifier.height(12.dp))
        if (state.notices.isEmpty()) {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "暂无公告",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            state.notices.take(3).forEach { notice ->
                NoticeRow(notice, onClick = { onOpenNotice(notice) })
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HeroButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = if (filled) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp, horizontal = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun NoticeRow(notice: Notice, onClick: () -> Unit) {
    SurfaceCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                notice.title.ifBlank { "公告" },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatDate(notice.created_at),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val preview = stripHtml(notice.content)
        if (preview.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun NoticeDialog(notice: Notice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(notice.title.ifBlank { "公告" }, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(formatDate(notice.created_at, withTime = true), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Text(stripHtml(notice.content), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}
