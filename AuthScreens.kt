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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xboard.app.AppViewModel
import com.xboard.app.data.Session
import com.xboard.app.ui.components.AppTextField
import com.xboard.app.ui.components.BrandMark
import com.xboard.app.ui.components.PrimaryButton
import com.xboard.app.ui.components.SurfaceCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun AuthScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
private fun ErrorText(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

@Composable
fun SetupScreen(
    viewModel: AppViewModel,
    initialUrl: String = "",
    onConnected: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(initialUrl) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthScaffold {
        Spacer(Modifier.height(40.dp))
        BrandMark(size = 76.dp)
        Spacer(Modifier.height(22.dp))
        Text("lio01", style = MaterialTheme.typography.headlineLarge)
        Text(
            "连接你的机场面板",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(36.dp))

        SurfaceCard {
            Text("服务器地址", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "填写面板域名即可，无需添加 /api/v1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            AppTextField(
                value = url,
                onValueChange = { url = it; error = null },
                label = "面板地址",
                placeholder = "https://example.com",
                leadingIcon = Icons.Rounded.Dns,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = "连接服务器",
                loading = loading,
                onClick = {
                    if (loading) return@PrimaryButton
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.connectServer(url)
                            onConnected()
                        } catch (e: Exception) {
                            error = e.message ?: "连接失败，请检查地址"
                        } finally {
                            loading = false
                        }
                    }
                },
            )
            ErrorText(error)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "支持所有基于 Xboard / V2Board 的面板\n账号、套餐、订阅与订单均保存在你的面板服务器",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    state: com.xboard.app.AppState,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onSwitchServer: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(state.session.email) }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthScaffold {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 52.dp)
            Spacer(Modifier.size(14.dp))
            Column {
                Text("欢迎回来", style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.session.host.ifBlank { "lio01" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(30.dp))

        SurfaceCard {
            Text("登录账号", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(18.dp))
            AppTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = "邮箱",
                placeholder = "you@example.com",
                leadingIcon = Icons.Rounded.AlternateEmail,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = "密码",
                placeholder = "至少 8 位",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            )
            ErrorText(error)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "登录",
                loading = loading,
                onClick = {
                    if (loading) return@PrimaryButton
                    if (email.isBlank() || password.isBlank()) {
                        error = "请输入邮箱和密码"
                        return@PrimaryButton
                    }
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.login(email, password)
                        } catch (e: Exception) {
                            error = e.message ?: "登录失败"
                        } finally {
                            loading = false
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onForgot) { Text("忘记密码") }
                TextButton(onClick = onRegister) { Text("注册账号") }
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onSwitchServer) {
            Text("切换服务器 · ${state.session.host}", style = MaterialTheme.typography.bodySmall)
        }
        if (state.captchaEnabled) {
            Text(
                "该面板已启用验证码，若登录失败请前往网页端登录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        val tos = state.config?.tos_url
        if (!tos.isNullOrBlank()) {
            Text(
                "登录即代表同意服务条款",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AppViewModel,
    state: com.xboard.app.AppState,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    if (countdown > 0) {
        androidx.compose.runtime.LaunchedEffect(countdown) {
            delay(1000)
            countdown -= 1
        }
    }

    AuthScaffold {
        Spacer(Modifier.height(8.dp))
        Text(
            "创建账号",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "注册后将自动登录",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        SurfaceCard {
            AppTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = "邮箱",
                placeholder = "you@example.com",
                leadingIcon = Icons.Rounded.AlternateEmail,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = "密码",
                placeholder = "至少 8 位",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
            )
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                label = "确认密码",
                placeholder = "再次输入密码",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
            )

            if (state.emailVerifyRequired) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AppTextField(
                        value = emailCode,
                        onValueChange = { emailCode = it; error = null },
                        label = "邮箱验证码",
                        placeholder = "6 位验证码",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(10.dp))
                    TextButton(
                        onClick = {
                            if (sending || countdown > 0) return@TextButton
                            if (email.isBlank()) {
                                error = "请先填写邮箱"
                                return@TextButton
                            }
                            sending = true
                            scope.launch {
                                try {
                                    viewModel.sendEmailVerifyCode(email)
                                    countdown = 60
                                } catch (e: Exception) {
                                    error = e.message ?: "发送失败"
                                } finally {
                                    sending = false
                                }
                            }
                        },
                        enabled = !sending && countdown == 0,
                    ) {
                        Text(if (countdown > 0) "${countdown}s" else "发送")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it; error = null },
                label = if (state.inviteForce) "邀请码（必填）" else "邀请码（选填）",
                placeholder = "邀请码",
            )

            ErrorText(error)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "注册并登录",
                loading = loading,
                onClick = {
                    if (loading) return@PrimaryButton
                    when {
                        email.isBlank() -> error = "请输入邮箱"
                        password.length < 8 -> error = "密码至少 8 位"
                        password != confirm -> error = "两次输入的密码不一致"
                        state.inviteForce && inviteCode.isBlank() -> error = "该面板需要邀请码"
                        state.emailVerifyRequired && emailCode.isBlank() -> error = "请输入邮箱验证码"
                        else -> {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    viewModel.register(email, password, inviteCode, emailCode)
                                } catch (e: Exception) {
                                    error = e.message ?: "注册失败"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("已有账号？返回登录") }
    }
}

@Composable
fun ForgotScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    if (countdown > 0) {
        androidx.compose.runtime.LaunchedEffect(countdown) {
            delay(1000)
            countdown -= 1
        }
    }

    AuthScaffold {
        Spacer(Modifier.height(8.dp))
        Text("重置密码", style = MaterialTheme.typography.headlineMedium)
        Text(
            "通过邮箱验证码重置",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        SurfaceCard {
            AppTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = "邮箱",
                placeholder = "you@example.com",
                leadingIcon = Icons.Rounded.AlternateEmail,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                AppTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = "验证码",
                    placeholder = "6 位验证码",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(10.dp))
                TextButton(
                    onClick = {
                        if (sending || countdown > 0) return@TextButton
                        if (email.isBlank()) {
                            error = "请先填写邮箱"
                            return@TextButton
                        }
                        sending = true
                        scope.launch {
                            try {
                                viewModel.sendEmailVerifyCode(email)
                                countdown = 60
                            } catch (e: Exception) {
                                error = e.message ?: "发送失败"
                            } finally {
                                sending = false
                            }
                        }
                    },
                    enabled = !sending && countdown == 0,
                ) {
                    Text(if (countdown > 0) "${countdown}s" else "发送")
                }
            }
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = "新密码",
                placeholder = "至少 8 位",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            )
            ErrorText(error)
            if (done) {
                Text(
                    "密码已重置，请返回登录",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "重置密码",
                loading = loading,
                onClick = {
                    if (loading) return@PrimaryButton
                    if (email.isBlank() || code.isBlank() || password.length < 8) {
                        error = "请完整填写邮箱、验证码和至少 8 位的新密码"
                        return@PrimaryButton
                    }
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.resetPassword(email, code, password)
                            done = true
                        } catch (e: Exception) {
                            error = e.message ?: "重置失败"
                        } finally {
                            loading = false
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("返回登录") }
    }
}
