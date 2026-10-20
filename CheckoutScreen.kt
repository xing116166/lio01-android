package com.xboard.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xboard.app.AppViewModel
import com.xboard.app.data.Order
import com.xboard.app.data.PaymentAction
import com.xboard.app.data.PaymentMethod
import com.xboard.app.ui.components.AppTopBar
import com.xboard.app.ui.components.InfoRow
import com.xboard.app.ui.components.LoadingView
import com.xboard.app.ui.components.PrimaryButton
import com.xboard.app.ui.components.SurfaceCard
import com.xboard.app.util.formatDate
import com.xboard.app.util.formatMoney
import com.xboard.app.util.openBrowser
import com.xboard.app.util.orderStatusText
import com.xboard.app.util.periodLabel
import com.xboard.app.util.generateQrCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CheckoutScreen(
    viewModel: AppViewModel,
    tradeNo: String,
    symbol: String,
    onBack: () -> Unit,
    onPaid: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var order by remember { mutableStateOf<Order?>(null) }
    var methods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var selectedMethod by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var paying by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf<PaymentAction?>(null) }
    var polling by remember { mutableStateOf(false) }
    var paid by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showRaw by remember { mutableStateOf(false) }
    var refreshingStatus by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        loadError = null
        try {
            val detail = viewModel.orderDetail(tradeNo)
            order = detail
            if (detail.total_amount > 0) {
                methods = viewModel.paymentMethods()
                if (selectedMethod == null) selectedMethod = methods.firstOrNull()?.id
            }
            if (detail.status >= 1) {
                paid = detail.status == 3 || detail.status == 1
            }
        } catch (e: Exception) {
            loadError = e.message ?: "订单加载失败"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(tradeNo) { load() }

    LaunchedEffect(polling) {
        if (!polling) return@LaunchedEffect
        var attempts = 0
        while (isActive && !paid && attempts < 120) {
            attempts++
            delay(2500)
            val status = runCatching { viewModel.checkOrder(tradeNo) }.getOrNull() ?: continue
            if (status >= 1) {
                paid = true
                polling = false
                viewModel.refreshUser()
                viewModel.refreshOrders()
                break
            }
        }
        if (!paid) polling = false
    }

    fun pay() {
        if (paying) return
        val current = order ?: return
        if (current.total_amount > 0 && selectedMethod == null) {
            actionError = "请选择支付方式"
            return
        }
        paying = true
        actionError = null
        scope.launch {
            try {
                val result = viewModel.checkout(tradeNo, selectedMethod.takeIf { current.total_amount > 0 })
                action = result
                when (result) {
                    is PaymentAction.Completed -> {
                        paid = true
                        viewModel.refreshUser()
                        viewModel.refreshOrders()
                    }
                    is PaymentAction.OpenUrl -> {
                        if (result.url.isNotBlank()) {
                            context.openBrowser(result.url)
                            polling = true
                        } else {
                            actionError = "支付链接为空，请重试"
                        }
                    }
                    is PaymentAction.QrCode -> {
                        showQr = true
                        polling = true
                    }
                    is PaymentAction.Raw -> {
                        showRaw = true
                        polling = true
                    }
                    PaymentAction.Unknown -> actionError = "该支付方式暂不支持，请更换支付方式"
                }
            } catch (e: Exception) {
                actionError = e.message ?: "发起支付失败"
            } finally {
                paying = false
            }
        }
    }

    if (paid) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            title = { Text("支付成功") },
            text = { Text("订阅已开通，流量与到期时间可能略有延迟，请稍后刷新查看。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { runCatching { viewModel.refreshUser() } }
                    onPaid()
                }) { Text("完成") }
            },
        )
    }

    val qrContent = (action as? PaymentAction.QrCode)?.content.orEmpty()
    if (showQr) {
        QrDialog(
            content = qrContent,
            onDismiss = { showQr = false },
            onCopy = { clipboard.setText(AnnotatedString(qrContent)) },
        )
    }
    if (showRaw) {
        val raw = (action as? PaymentAction.Raw)?.content.orEmpty()
        AlertDialog(
            onDismissRequest = { showRaw = false },
            title = { Text("继续完成支付") },
            text = {
                Column(Modifier.height(220.dp).verticalScroll(rememberScrollState())) {
                    Text(raw, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { clipboard.setText(AnnotatedString(raw)) }) { Text("复制") }
            },
            dismissButton = { TextButton(onClick = { showRaw = false }) { Text("关闭") } },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "确认订单",
                onBack = onBack,
                actions = {
                    if (order?.status == 0 && polling) {
                        TextButton(onClick = { polling = false }) { Text("暂停检测", style = MaterialTheme.typography.labelMedium) }
                    }
                },
            )
        },
        bottomBar = {
            if (order != null && !paid) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    if (actionError != null) {
                        Text(
                            actionError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    if (polling) {
                        Text(
                            "正在检测支付结果… 完成支付后会自动开通",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("应付", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if ((order?.total_amount ?: 0) <= 0) "余额支付" else formatMoney(order?.total_amount, symbol),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        PrimaryButton(
                            text = when {
                                paying -> "处理中…"
                                (order?.total_amount ?: 0) <= 0 -> "确认开通"
                                else -> "去支付"
                            },
                            loading = paying,
                            modifier = Modifier.width(170.dp),
                            onClick = { pay() },
                        )
                    }
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
            when {
                loading -> LoadingView()
                loadError != null -> Column(Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(10.dp))
                    Text(loadError.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { scope.launch { load() } }) { Text("重试") }
                }
                order != null -> {
                    val detail = order!!
                    Spacer(Modifier.height(8.dp))
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                detail.plan?.name ?: "订阅订单",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                orderStatusText(detail.status),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        InfoRow("订单号", detail.trade_no)
                        InfoRow("周期", periodLabel(detail.period))
                        InfoRow("创建时间", formatDate(detail.created_at, withTime = true))
                        InfoRow("订单金额", formatMoney(detail.total_amount, symbol))
                        if ((detail.balance_amount ?: 0) > 0) {
                            InfoRow("余额抵扣", "-" + formatMoney(detail.balance_amount, symbol))
                        }
                        InfoRow(
                            "应付金额",
                            if (detail.total_amount <= 0) "0（余额已抵扣）" else formatMoney(detail.total_amount, symbol),
                            valueColor = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    if (detail.status == 0) {
                        if (detail.total_amount <= 0) {
                            SurfaceCard {
                                Text("该订单无需额外支付", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "余额已抵扣全部金额，点击下方按钮即可开通。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Text("支付方式", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            if (methods.isEmpty()) {
                                SurfaceCard {
                                    Text(
                                        "面板暂未配置可用的支付方式，请联系管理员。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                methods.forEach { method ->
                                    PaymentRow(
                                        method = method,
                                        symbol = symbol,
                                        total = detail.total_amount,
                                        selected = selectedMethod == method.id,
                                        onClick = { selectedMethod = method.id; actionError = null },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }

                        if (polling) {
                            Spacer(Modifier.height(16.dp))
                            SurfaceCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("等待支付结果", style = MaterialTheme.typography.titleSmall)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "在浏览器 / 二维码中完成支付后，订单会自动开通。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TextButton(
                                        onClick = {
                                            val url = (action as? PaymentAction.OpenUrl)?.url
                                            if (!url.isNullOrBlank()) context.openBrowser(url)
                                        },
                                    ) { Text("重新打开支付页") }
                                    TextButton(
                                        enabled = !refreshingStatus,
                                        onClick = {
                                            refreshingStatus = true
                                            scope.launch {
                                                try {
                                                    val status = viewModel.checkOrder(tradeNo)
                                                    if (status >= 1) {
                                                        paid = true
                                                        viewModel.refreshUser()
                                                        viewModel.refreshOrders()
                                                    }
                                                } catch (_: Exception) {
                                                } finally {
                                                    refreshingStatus = false
                                                }
                                            }
                                        },
                                    ) { Text(if (refreshingStatus) "检测中…" else "我已支付，检测结果") }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        viewModel.cancelOrder(tradeNo)
                                        onBack()
                                    } catch (e: Exception) {
                                        actionError = e.message ?: "取消失败"
                                    }
                                }
                            },
                        ) { Text("取消订单", color = MaterialTheme.colorScheme.error) }
                    } else {
                        SurfaceCard {
                            Text(
                                if (detail.status == 3) "订单已完成" else "订单${orderStatusText(detail.status)}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "当前订单状态：${orderStatusText(detail.status)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(
    method: PaymentMethod,
    symbol: String,
    total: Long,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val feePercent = method.handling_fee_percent ?: 0.0
    val feeFixed = method.handling_fee_fixed ?: 0L
    val fee = if (feePercent > 0 || feeFixed > 0) {
        (total * feePercent / 100.0).toLong() + feeFixed
    } else {
        0L
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(method.name.ifBlank { method.payment }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (fee > 0) {
                Text(
                    "手续费 ${formatMoney(fee, symbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (fee > 0) {
            Text(
                formatMoney(total + fee, symbol),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun QrDialog(content: String, onDismiss: () -> Unit, onCopy: () -> Unit) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, content) {
        value = withContext(Dispatchers.Default) { generateQrCode(content) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("扫码支付") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = bitmap
                    if (image != null) {
                        Image(bitmap = image, contentDescription = "支付二维码", modifier = Modifier.fillMaxSize())
                    } else {
                        Text("二维码生成中…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "请使用对应 App 扫码完成支付",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onCopy) { Text("复制内容") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
