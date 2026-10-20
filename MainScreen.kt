package com.xboard.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.xboard.app.AppState
import com.xboard.app.AppViewModel
import com.xboard.app.data.Notice
import com.xboard.app.data.Plan
import kotlinx.coroutines.launch

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(
    state: AppState,
    viewModel: AppViewModel,
    onOpenPlan: (Plan) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenInvite: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPassword: () -> Unit,
    onLogout: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<Notice?>(null) }
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        TabItem("首页", Icons.Rounded.Home),
        TabItem("套餐", Icons.Rounded.ShoppingBag),
        TabItem("节点", Icons.Rounded.Dns),
        TabItem("我的", Icons.Rounded.Person),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    state = state,
                    onBuy = { tab = 1 },
                    onOrders = onOpenOrders,
                    onNodes = { tab = 2 },
                    onInvite = onOpenInvite,
                    onSettings = onOpenSettings,
                    onRefresh = { scope.launch { viewModel.refreshAll() } },
                    onOpenNotice = { notice = it },
                )
                1 -> PlansScreen(
                    state = state,
                    onPlanClick = onOpenPlan,
                    onRefresh = { viewModel.refreshAll() },
                )
                2 -> NodesScreen(
                    state = state,
                    onRefresh = { viewModel.refreshAll() },
                    onBuy = { tab = 1 },
                )
                else -> ProfileScreen(
                    state = state,
                    onOrders = onOpenOrders,
                    onInvite = onOpenInvite,
                    onNotices = onOpenNotices,
                    onPassword = onOpenPassword,
                    onSettings = onOpenSettings,
                    onLogout = onLogout,
                )
            }
        }
    }

    notice?.let {
        NoticeDialog(notice = it, onDismiss = { notice = null })
    }
}
