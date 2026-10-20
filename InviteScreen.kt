package com.xboard.app.ui.screens

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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xboard.app.AppViewModel
import com.xboard.app.data.InviteInfo
import com.xboard.app.ui.components.AppTopBar
import com.xboard.app.ui.components.LoadingView
import com.xboard.app.ui.components.PrimaryButton
import com.xboard.app.ui.components.SurfaceCard
import com.xboard.app.util.formatMoney
import com.xboard.app.util.shareText
import com.xboard.app.ui.theme.HeroGradient
import kotlinx.coroutines.launch

@Composable
fun InviteScreen(
    viewModel: AppViewModel,
    symbol: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var info by remember { mutableStateOf<InviteInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var generating by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            info = viewModel.inviteInfo()
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
        } finally {
            loading = false
        }
    }

    val code = info?.codes?.firstOrNull()?.code.orEmpty()
    val link = if (code.isBlank()) "" else "${viewModel.state.value.session.baseUrl}/#/register?code=$code"
    val stat = info?.stat.orEmpty()
    val registered = stat.getOrNull(0) ?: 0L
    val validCommission = stat.getOrNull(1) ?: 0L
    val pendingCommission = stat.getOrNull(2) ?: 0L
    val rate = stat.getOrNull(3) ?: 0L
    val available = stat.getOrNull(4) ?: 0L

    Scaffold(
        topBar = { AppTopBar(title = "邀请好友", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            when {
                loading -> LoadingView()
                error != null -> Column(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { refreshKey++ }) { Text("重试") }
                }
                else -> {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(HeroGradient))
                            .padding(22.dp),
                    ) {
                        Column {
                            Text("邀请返佣", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "邀请好友注册，最高可获得 $rate% 返佣",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(16.dp))
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text("已邀请", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                    Text("$registered 人", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("可提现佣金", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                    Text(formatMoney(available, symbol), color = Color.White, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    SurfaceCard {
                        Text("我的邀请码", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        if (code.isBlank()) {
                            Text(
                                "暂未生成邀请码",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Text(code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) { Text("复制") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                link,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(
                                text = "生成新邀请码",
                                loading = generating,
                                enabled = !generating && (info?.codes?.size ?: 0) < 5,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    generating = true
                                    scope.launch {
                                        try {
                                            viewModel.createInviteCode()
                                            refreshKey++
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            generating = false
                                        }
                                    }
                                },
                            )
                            OutlinedAction(
                                text = "分享链接",
                                icon = Icons.Rounded.Share,
                                modifier = Modifier.weight(1f),
                                enabled = link.isNotBlank(),
                                onClick = { if (link.isNotBlank()) context.shareText(link, "邀请注册") },
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    SurfaceCard {
                        Text("佣金明细", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        InfoStat("有效佣金", formatMoney(validCommission, symbol))
                        InfoStat("确认中佣金", formatMoney(pendingCommission, symbol))
                        InfoStat("返佣比例", "$rate%")
                    }

                    Spacer(Modifier.height(14.dp))
                    if (!info?.codes.isNullOrEmpty()) {
                        SurfaceCard {
                            Text("邀请码列表", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            info!!.codes.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.code, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        if (item.status == 0) "可用" else "已使用",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.status == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Rounded.ContentCopy,
                                        contentDescription = "复制",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .clip(CircleShape)
                                            .clickable { clipboard.setText(AnnotatedString(item.code)) },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "邀请链接仅用于注册，佣金规则由面板管理员配置。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OutlinedAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
