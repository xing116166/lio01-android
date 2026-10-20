package com.xboard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xboard.app.AppConfig
import com.xboard.app.AppViewModel
import com.xboard.app.UiEvent
import com.xboard.app.ui.components.BrandMark
import com.xboard.app.ui.components.LoadingView
import com.xboard.app.ui.screens.CheckoutScreen
import com.xboard.app.ui.screens.ForgotScreen
import com.xboard.app.ui.screens.InviteScreen
import com.xboard.app.ui.screens.LoginScreen
import com.xboard.app.ui.screens.MainScreen
import com.xboard.app.ui.screens.NoticesScreen
import com.xboard.app.ui.screens.OrdersScreen
import com.xboard.app.ui.screens.PasswordScreen
import com.xboard.app.ui.screens.PlanDetailScreen
import com.xboard.app.ui.screens.RegisterScreen
import com.xboard.app.ui.screens.SettingsScreen
import com.xboard.app.ui.screens.SetupScreen
import com.xboard.app.ui.theme.HeroGradient
import kotlinx.coroutines.launch

object Routes {
    const val SETUP = "setup"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val MAIN = "main"
    const val PLAN = "plan/{planId}"
    const val CHECKOUT = "checkout/{tradeNo}"
    const val ORDERS = "orders"
    const val INVITE = "invite"
    const val NOTICES = "notices"
    const val SETTINGS = "settings"
    const val PASSWORD = "password"

    fun plan(id: Int) = "plan/$id"
    fun checkout(tradeNo: String) = "checkout/$tradeNo"
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(size = 84.dp)
            Spacer(Modifier.height(18.dp))
            Text("lio01", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "你的订阅客户端",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun MissingPlanScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingView()
        Text(
            "套餐信息加载中…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = onBack) { Text("返回") }
    }
}

@Composable
fun XboardRoot(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                UiEvent.LoggedIn -> navController.navigateClearingStack(Routes.MAIN)
                UiEvent.LoggedOut, UiEvent.SessionExpired -> navController.navigateClearingStack(Routes.LOGIN)
            }
        }
    }

    if (!state.ready) {
        SplashScreen()
        return
    }

    val startDestination = remember {
        when {
            state.session.baseUrl.isBlank() -> Routes.SETUP
            state.session.token.isBlank() -> Routes.LOGIN
            else -> Routes.MAIN
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.SETUP) {
                SetupScreen(
                    viewModel = viewModel,
                    initialUrl = state.session.baseUrl.ifBlank { AppConfig.DEFAULT_BASE_URL },
                    onConnected = {
                        navController.navigateClearingStack(Routes.LOGIN)
                    },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = viewModel,
                    state = state,
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onForgot = { navController.navigate(Routes.FORGOT) },
                    onSwitchServer = { navController.navigate(Routes.SETUP) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    viewModel = viewModel,
                    state = state,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.FORGOT) {
                ForgotScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.MAIN) {
                MainScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenPlan = { plan -> navController.navigate(Routes.plan(plan.id)) },
                    onOpenOrders = { navController.navigate(Routes.ORDERS) },
                    onOpenOrder = { tradeNo -> navController.navigate(Routes.checkout(tradeNo)) },
                    onOpenInvite = { navController.navigate(Routes.INVITE) },
                    onOpenNotices = { navController.navigate(Routes.NOTICES) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenPassword = { navController.navigate(Routes.PASSWORD) },
                    onLogout = { scope.launch { viewModel.logout() } },
                )
            }
            composable(
                route = Routes.PLAN,
                arguments = listOf(navArgument("planId") { type = NavType.IntType }),
            ) { entry ->
                val planId = entry.arguments?.getInt("planId") ?: 0
                val plan = state.plans.firstOrNull { it.id == planId }
                if (plan == null) {
                    LaunchedEffect(planId) {
                        runCatching { viewModel.refreshAll() }
                    }
                    MissingPlanScreen(onBack = { navController.popBackStack() })
                } else {
                    PlanDetailScreen(
                        viewModel = viewModel,
                        plan = plan,
                        symbol = state.currencySymbol,
                        onBack = { navController.popBackStack() },
                        onCreated = { tradeNo -> navController.navigate(Routes.checkout(tradeNo)) },
                    )
                }
            }
            composable(
                route = Routes.CHECKOUT,
                arguments = listOf(navArgument("tradeNo") { type = NavType.StringType }),
            ) { entry ->
                val tradeNo = entry.arguments?.getString("tradeNo").orEmpty()
                CheckoutScreen(
                    viewModel = viewModel,
                    tradeNo = tradeNo,
                    symbol = state.currencySymbol,
                    onBack = { navController.popBackStack() },
                    onPaid = {
                        navController.popBackStack(Routes.MAIN, inclusive = false)
                    },
                )
            }
            composable(Routes.ORDERS) {
                OrdersScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenOrder = { tradeNo -> navController.navigate(Routes.checkout(tradeNo)) },
                )
            }
            composable(Routes.INVITE) {
                InviteScreen(
                    viewModel = viewModel,
                    symbol = state.currencySymbol,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.NOTICES) {
                NoticesScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    state = state,
                    onBack = { navController.popBackStack() },
                    onServerChanged = {
                        navController.navigateClearingStack(Routes.LOGIN)
                    },
                    onLogout = { scope.launch { viewModel.logout() } },
                )
            }
            composable(Routes.PASSWORD) {
                PasswordScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}


private fun NavHostController.navigateClearingStack(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}
