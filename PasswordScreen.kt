package com.xboard.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xboard.app.AppViewModel
import com.xboard.app.ui.components.AppTextField
import com.xboard.app.ui.components.AppTopBar
import com.xboard.app.ui.components.PrimaryButton
import com.xboard.app.ui.components.SurfaceCard
import kotlinx.coroutines.launch

@Composable
fun PasswordScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar(title = "修改密码", onBack = onBack) },
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
            Spacer(Modifier.height(10.dp))
            Text(
                "为了账号安全，建议定期更换密码",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SurfaceCard {
                AppTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; error = null },
                    label = "当前密码",
                    placeholder = "输入当前密码",
                    leadingIcon = Icons.Rounded.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                )
                Spacer(Modifier.height(16.dp))
                AppTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = "新密码",
                    placeholder = "至少 8 位",
                    leadingIcon = Icons.Rounded.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                )
                Spacer(Modifier.height(16.dp))
                AppTextField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = "确认新密码",
                    placeholder = "再次输入新密码",
                    leadingIcon = Icons.Rounded.Lock,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                )

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (success) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "密码修改成功",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = "保存修改",
                    loading = loading,
                    onClick = {
                        if (loading) return@PrimaryButton
                        when {
                            oldPassword.isBlank() -> error = "请输入当前密码"
                            newPassword.length < 8 -> error = "新密码至少 8 位"
                            newPassword != confirm -> error = "两次输入的新密码不一致"
                            else -> {
                                loading = true
                                error = null
                                success = false
                                scope.launch {
                                    try {
                                        viewModel.changePassword(oldPassword, newPassword)
                                        success = true
                                        oldPassword = ""
                                        newPassword = ""
                                        confirm = ""
                                    } catch (e: Exception) {
                                        error = e.message ?: "修改失败"
                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
