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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xboard.app.AppState
import com.xboard.app.AppViewModel
import com.xboard.app.BuildConfig
import com.xboard.app.ui.components.AppTextField
import com.xboard.app.ui.components.AppTopBar
import com.xboard.app.ui.components.SurfaceCard
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    state: AppState,
    onBack: () -> Unit,
    onServerChanged: () -> Unit,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showServerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar(title = "设置", onBack = onBack) },
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
            Spacer(Modifier.height(8.dp))
            Text("服务器", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            SurfaceCard(modifier = Modifier.clickable { showServerDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("面板地址", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            state.session.baseUrl.ifBlank { "未设置" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("更换", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                "切换服务器会退出当前账号",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )

            Spacer(Modifier.height(22.dp))
            Text("外观", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            SurfaceCard {
                ThemeRow("跟随系统", "system", state.session.theme) {
                    scope.launch { viewModel.updateTheme("system") }
                }
                ThemeRow("浅色", "light", state.session.theme) {
                    scope.launch { viewModel.updateTheme("light") }
                }
                ThemeRow("深色", "dark", state.session.theme) {
                    scope.launch { viewModel.updateTheme("dark") }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("关于", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            SurfaceCard {
                SettingsRow("版本", "v${BuildConfig.VERSION_NAME}")
                SettingsRow("面板", state.config?.app_description ?: "--")
                SettingsRow("API", "${state.session.baseUrl}/api/v1")
                SettingsRow("协议", "Xboard / V2Board 兼容")
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    .clickable { showLogoutDialog = true }
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("退出登录", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showServerDialog) {
        ServerDialog(
            initial = state.session.baseUrl,
            onDismiss = { showServerDialog = false },
            onConfirm = { url, onError, done ->
                scope.launch {
                    try {
                        viewModel.connectServer(url)
                        done()
                        showServerDialog = false
                        onServerChanged()
                    } catch (e: Exception) {
                        onError(e.message ?: "连接失败")
                    }
                }
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    scope.launch { onLogout() }
                }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ThemeRow(label: String, mode: String, current: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = current == mode, onClick = onClick)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (current == mode) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun ServerDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String, (String) -> Unit, () -> Unit) -> Unit,
) {
    var url by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更换服务器") },
        text = {
            Column {
                AppTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = "面板地址",
                    placeholder = "https://example.com",
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "切换服务器会清除当前登录状态",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    loading = true
                    onConfirm(url, { message ->
                        loading = false
                        error = message
                    }, { loading = false })
                },
            ) { Text(if (loading) "连接中…" else "连接") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
