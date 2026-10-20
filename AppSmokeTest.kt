package com.xboard.app

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xboard.app.data.Session
import com.xboard.app.data.SessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boots the real MainActivity in Robolectric and verifies that a configured
 * panel lands on the login screen without crashing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSmokeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun configuredPanelShowsLogin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            SessionStore(context).save(
                Session(
                    baseUrl = "http://127.0.0.1:1",
                    token = "",
                    email = "demo@xboard.dev",
                    theme = "light",
                ),
            )
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithText("欢迎回来").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText("登录").fetchSemanticsNodes()
        }
    }

    @Test
    fun freshInstallUsesDefaultPanel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { SessionStore(context).save(Session()) }

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithText("airport.limx01.com").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
