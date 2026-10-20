package com.xboard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xboard.app.AppState
import com.xboard.app.AppViewModel
import com.xboard.app.data.Plan
import com.xboard.app.ui.components.AppTextField
import com.xboard.app.ui.components.AppTopBar
import com.xboard.app.ui.components.EmptyState
import com.xboard.app.ui.components.InfoRow
import com.xboard.app.ui.components.PrimaryButton
import com.xboard.app.ui.components.SurfaceCard
import com.xboard.app.ui.components.TagPill
import com.xboard.app.util.PeriodOption
import com.xboard.app.util.availablePeriods
import com.xboard.app.util.formatPlanTraffic
import com.xboard.app.util.lowestPrice
import com.xboard.app.util.periodLabel
import com.xboard.app.util.stripHtml
import kotlinx.coroutines.launch

@Composable
fun PlansScreen(
    state: AppState,
    onPlanClick: (Plan) -> Unit,
    onRefresh: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("套餐", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "选择适合你的订阅计划",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (state.plans.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.ShoppingBag,
                    title = "暂无可购买的套餐",
                    subtitle = "下拉刷新，或稍后再来看看",
                    action = {
                        PrimaryButton(
                            text = if (refreshing) "刷新中…" else "刷新",
                            enabled = !refreshing,
                            modifier = Modifier.width(160.dp),
                            onClick = {
                                refreshing = true
                                scope.launch {
                                    try {
                                        onRefresh()
                                    } finally {
                                        refreshing = false
                                    }
                                }
                            },
                        )
                    },
                )
            }
        } else {
            items(state.plans, key = { it.id }) { plan ->
                PlanCard(plan = plan, symbol = state.currencySymbol, onClick = { onPlanClick(plan) })
            }
        }
    }
}

@Composable
fun PlanCard(plan: Plan, symbol: String, onClick: () -> Unit) {
    SurfaceCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    plan.name.ifBlank { "套餐 #${plan.id}" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val tags = plan.tags.orEmpty().filter { it.isNotBlank() }
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.take(3).forEach { TagPill(it) }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    plan.lowestPrice(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        val description = stripHtml(plan.content)
        if (description.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PlanFeature(Icons.Rounded.Wifi, formatPlanTraffic(plan.transfer_enable))
            PlanFeature(
                Icons.Rounded.Devices,
                plan.device_limit?.takeIf { it > 0 }?.let { "$it 台设备" } ?: "设备不限",
            )
            PlanFeature(
                Icons.Rounded.Speed,
                plan.speed_limit?.takeIf { it > 0 }?.let { "$it Mbps" } ?: "不限速",
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "查看详情",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun PlanFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PlanDetailScreen(
    viewModel: AppViewModel,
    plan: Plan,
    symbol: String,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val periods = remember(plan.id) { plan.availablePeriods() }
    var selected by remember(plan.id) { mutableStateOf(periods.firstOrNull()) }
    var coupon by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { AppTopBar(title = "套餐详情", onBack = onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                if (error != null) {
                    Text(
                        error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("应付金额", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            selected?.priceCents?.let { "$symbol${"%.2f".format(it / 100)}" } ?: "--",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    PrimaryButton(
                        text = if (plan.sell == false) "暂停销售" else "立即订阅",
                        loading = loading,
                        enabled = selected != null && plan.sell != false,
                        modifier = Modifier.width(170.dp),
                        onClick = {
                            if (loading) return@PrimaryButton
                            val option = selected ?: return@PrimaryButton
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    val tradeNo = viewModel.createOrder(plan.id, option.key, coupon)
                                    onCreated(tradeNo)
                                } catch (e: Exception) {
                                    error = e.message ?: "创建订单失败"
                                } finally {
                                    loading = false
                                }
                            }
                        },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                plan.name.ifBlank { "套餐 #${plan.id}" },
                style = MaterialTheme.typography.headlineMedium,
            )
            val tags = plan.tags.orEmpty().filter { it.isNotBlank() }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { TagPill(it) }
                }
            }
            Spacer(Modifier.height(16.dp))

            val description = stripHtml(plan.content)
            if (description.isNotBlank()) {
                SurfaceCard {
                    Text("套餐说明", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            SurfaceCard {
                Text("套餐配置", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                InfoRow("流量", formatPlanTraffic(plan.transfer_enable) + " / 周期")
                InfoRow("设备限制", plan.device_limit?.takeIf { it > 0 }?.let { "$it 台" } ?: "不限制")
                InfoRow("速度限制", plan.speed_limit?.takeIf { it > 0 }?.let { "$it Mbps" } ?: "不限制")
                InfoRow("流量重置", resetMethodText(plan.reset_traffic_method))
                InfoRow("订阅人数上限", plan.capacity_limit?.toString()?.trim('"')?.takeIf { it != "null" } ?: "不限")
            }

            Spacer(Modifier.height(20.dp))
            Text("选择周期", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            if (periods.isEmpty()) {
                Text(
                    "该套餐暂未配置价格",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                periods.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { option ->
                            PeriodSelectable(
                                option = option,
                                symbol = symbol,
                                selected = selected?.key == option.key,
                                modifier = Modifier.weight(1f),
                                onClick = { selected = option },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = coupon,
                onValueChange = { coupon = it; error = null },
                label = "优惠券（选填）",
                placeholder = "输入优惠码，下单时自动校验",
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PeriodSelectable(
    option: PeriodOption,
    symbol: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            periodLabel(option.key),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "$symbol${"%.2f".format((option.priceCents ?: 0.0) / 100)}",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resetMethodText(method: Int?): String = when (method) {
    0 -> "每月 1 号"
    1 -> "按月重置"
    2 -> "不重置"
    3 -> "每年 1 月 1 日"
    4 -> "按年重置"
    else -> "跟随系统"
}
