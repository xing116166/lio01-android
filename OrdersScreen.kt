package com.xboard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xboard.app.AppState
import com.xboard.app.AppViewModel
import com.xboard.app.data.Order
import com.xboard.app.ui.components.AppTopBar
import com.xboard.app.ui.components.EmptyState
import com.xboard.app.ui.components.InfoRow
import com.xboard.app.ui.components.SurfaceCard
import com.xboard.app.util.formatDate
import com.xboard.app.util.formatMoney
import com.xboard.app.util.orderStatusText
import com.xboard.app.util.orderTypeText
import com.xboard.app.util.periodLabel
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(
    state: AppState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<Order?>(null) }
    var detailTarget by remember { mutableStateOf<Order?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "我的订单",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            if (refreshing) return@IconButton
                            refreshing = true
                            scope.launch {
                                try {
                                    viewModel.refreshOrders()
                                } catch (e: Exception) {
                                    message = e.message ?: "刷新失败"
                                } finally {
                                    refreshing = false
                                }
                            }
                        },
                    ) {
                        if (refreshing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.orders.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "还没有订单",
                        subtitle = "购买套餐后订单会显示在这里",
                    )
                }
            } else {
                items(state.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        symbol = state.currencySymbol,
                        onPay = { onOpenOrder(order.trade_no) },
                        onCancel = { cancelTarget = order },
                        onDetail = { detailTarget = order },
                    )
                }
            }
            if (message != null) {
                item {
                    Text(
                        message.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (cancelTarget != null) {
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("取消订单") },
            text = { Text("确定取消订单 ${cancelTarget?.trade_no} 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val target = cancelTarget
                    cancelTarget = null
                    if (target != null) {
                        scope.launch {
                            try {
                                viewModel.cancelOrder(target.trade_no)
                            } catch (e: Exception) {
                                message = e.message ?: "取消失败"
                            }
                        }
                    }
                }) { Text("取消订单", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { cancelTarget = null }) { Text("再想想") } },
        )
    }

    if (detailTarget != null) {
        val order = detailTarget!!
        AlertDialog(
            onDismissRequest = { detailTarget = null },
            title = { Text("订单详情") },
            text = {
                Column {
                    InfoRow("订单号", order.trade_no)
                    InfoRow("套餐", order.plan?.name ?: "#${order.plan_id}")
                    InfoRow("类型", orderTypeText(order.type))
                    InfoRow("周期", periodLabel(order.period))
                    InfoRow("金额", formatMoney(order.total_amount, state.currencySymbol))
                    InfoRow("状态", orderStatusText(order.status))
                    InfoRow("创建时间", formatDate(order.created_at, withTime = true))
                    if ((order.paid_at ?: 0) > 0) {
                        InfoRow("支付时间", formatDate(order.paid_at, withTime = true))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { detailTarget = null }) { Text("关闭") } },
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    symbol: String,
    onPay: () -> Unit,
    onCancel: () -> Unit,
    onDetail: () -> Unit,
) {
    val statusColor = when (order.status) {
        0 -> MaterialTheme.colorScheme.tertiary
        1 -> MaterialTheme.colorScheme.primary
        3 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    order.plan?.name ?: "订阅订单",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${orderTypeText(order.type)} · ${periodLabel(order.period)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.14f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(orderStatusText(order.status), style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatMoney(order.total_amount, symbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    formatDate(order.created_at, withTime = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (order.status == 0) {
                TextButton(onClick = onCancel) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onPay) { Text("去支付", style = MaterialTheme.typography.labelMedium) }
            } else {
                TextButton(onClick = onDetail) { Text("详情", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
