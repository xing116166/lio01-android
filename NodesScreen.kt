package com.xboard.app.ui.screens

import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xboard.app.AppState
import com.xboard.app.ui.components.EmptyState
import com.xboard.app.ui.components.LoadingView
import com.xboard.app.ui.components.PrimaryButton
import com.xboard.app.ui.components.SurfaceCard
import com.xboard.app.ui.components.TagPill
import com.xboard.app.util.generateQrCode
import com.xboard.app.util.openBrowser
import com.xboard.app.util.shareText
import com.xboard.app.vpn.ProxyController
import com.xboard.app.vpn.ProxyState
import com.xboard.app.vpn.ProxyStatus
import com.xboard.app.vpn.VpnNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NodesScreen(
    state: AppState,
    onRefresh: suspend () -> Unit,
    onLoadNodes: suspend () -> Unit,
    onSelectNode: (String) -> Unit,
    onBuy: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    val url = state.subscribe?.subscribe_url.orEmpty()
    val proxyStatus by ProxyController.status.collectAsState()
    val selectedNode = state.vpnNodes.firstOrNull { it.id == state.selectedVpnNodeId }
        ?: state.vpnNodes.firstOrNull()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val node = selectedNode
        if (result.resultCode == android.app.Activity.RESULT_OK && node != null) {
            ProxyController.start(context, node)
        }
    }

    fun toggleProxy() {
        when (proxyStatus.state) {
            ProxyState.CONNECTED, ProxyState.STARTING -> ProxyController.stop(context)
            else -> {
                val node = selectedNode ?: return
                val prepare = VpnService.prepare(context)
                if (prepare != null) {
                    vpnPermissionLauncher.launch(prepare)
                } else {
                    ProxyController.start(context, node)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (state.vpnNodes.isEmpty() && state.subscribe?.subscribe_url?.isNotBlank() == true) {
            onLoadNodes()
        }
    }

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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("订阅节点", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "选择节点并开启系统代理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        if (refreshing) return@IconButton
                        refreshing = true
                        scope.launch {
                            try {
                                onRefresh()
                                onLoadNodes()
                            } finally {
                                refreshing = false
                            }
                        }
                    },
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                }
            }
        }

        item { ProxyCard(proxyStatus, selectedNode, onToggle = { toggleProxy() }) }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("订阅链接", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (url.isBlank()) "购买套餐后生成订阅链接" else url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill("复制", Icons.Rounded.ContentCopy, Modifier.weight(1f)) {
                        if (url.isNotBlank()) clipboard.setText(AnnotatedString(url))
                    }
                    ActionPill("二维码", Icons.Rounded.QrCode2, Modifier.weight(1f)) {
                        if (url.isNotBlank()) showQr = true
                    }
                    ActionPill("分享", Icons.Rounded.Share, Modifier.weight(1f)) {
                        if (url.isNotBlank()) context.shareText(url, "分享订阅链接")
                    }
                }
                if (url.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { context.openBrowser(url) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text("在浏览器中打开", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("可用节点", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${state.vpnNodes.size} 个",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            state.vpnLoading -> item { LoadingView() }
            state.vpnError != null -> item {
                EmptyState(
                    icon = Icons.Rounded.Dns,
                    title = "节点加载失败",
                    subtitle = state.vpnError,
                    action = {
                        TextButton(onClick = { scope.launch { onLoadNodes() } }) { Text("重试") }
                    },
                )
            }
            state.vpnNodes.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Rounded.Dns,
                    title = "暂无可用节点",
                    subtitle = "订阅生效后节点会显示在这里",
                    action = {
                        TextButton(onClick = onBuy) { Text("去购买套餐") }
                    },
                )
            }
            else -> items(state.vpnNodes, key = { it.id }) { node ->
                VpnNodeRow(
                    node = node,
                    selected = node.id == selectedNode?.id,
                    onClick = { onSelectNode(node.id) },
                )
            }
        }
    }

    if (showQr) {
        SubscriptionQrDialog(url = url, onDismiss = { showQr = false })
    }
}

@Composable
private fun ProxyCard(
    status: ProxyStatus,
    selected: VpnNode?,
    onToggle: () -> Unit,
) {
    val starting = status.state == ProxyState.STARTING
    val statusColor = when (status.state) {
        ProxyState.CONNECTED -> Color(0xFF22C55E)
        ProxyState.STARTING -> MaterialTheme.colorScheme.primary
        ProxyState.ERROR -> MaterialTheme.colorScheme.error
        ProxyState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val title = when (status.state) {
        ProxyState.CONNECTED -> "已连接 · ${status.nodeName}"
        ProxyState.STARTING -> "正在连接 · ${status.nodeName}"
        ProxyState.ERROR -> "连接失败"
        ProxyState.STOPPED -> "代理未连接"
    }

    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(statusColor))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("代理内核", style = MaterialTheme.typography.titleSmall)
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (starting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        if (status.state == ProxyState.ERROR && status.message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                status.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            selected?.let { "当前节点：${it.name}" } ?: "请选择一个节点",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                when (status.state) {
                    ProxyState.CONNECTED -> "全局代理运行中"
                    ProxyState.STARTING -> "正在建立 VPN 通道"
                    else -> "开启后所有流量经节点转发"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = when (status.state) {
                    ProxyState.CONNECTED -> "断开"
                    ProxyState.STARTING -> "停止"
                    else -> "连接"
                },
                modifier = Modifier.width(120.dp),
                enabled = selected != null || status.state != ProxyState.STOPPED,
                onClick = onToggle,
            )
        }
    }
}

@Composable
private fun VpnNodeRow(node: VpnNode, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(
                node.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${node.protocol.uppercase()} · ${node.server}:${node.port}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        TagPill(node.protocol.uppercase())
    }
}

@Composable
private fun ActionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SubscriptionQrDialog(url: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.Default) { generateQrCode(url) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("订阅二维码") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = bitmap
                    if (image != null) {
                        Image(bitmap = image, contentDescription = "订阅二维码", modifier = Modifier.fillMaxSize())
                    } else {
                        Text("生成中…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "使用代理客户端扫描二维码导入订阅",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(url)) }) { Text("复制链接") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
