package com.xboard.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "xboard_session")

data class Session(
    val baseUrl: String = "",
    val token: String = "",
    val email: String = "",
    val theme: String = "system",
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()

    val authHeader: String?
        get() = when {
            token.isBlank() -> null
            token.startsWith("Bearer ", ignoreCase = true) -> token
            else -> "Bearer $token"
        }

    val host: String
        get() = baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
}

class SessionStore(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
        val EMAIL = stringPreferencesKey("email")
        val THEME = stringPreferencesKey("theme")
    }

    val session: Flow<Session> = context.sessionDataStore.data.map { p ->
        Session(
            baseUrl = p[Keys.BASE_URL].orEmpty(),
            token = p[Keys.TOKEN].orEmpty(),
            email = p[Keys.EMAIL].orEmpty(),
            theme = p[Keys.THEME] ?: "system",
        )
    }

    suspend fun current(): Session = session.first()

    suspend fun save(session: Session) {
        context.sessionDataStore.edit { p ->
            p[Keys.BASE_URL] = session.baseUrl
            p[Keys.TOKEN] = session.token
            p[Keys.EMAIL] = session.email
            p[Keys.THEME] = session.theme
        }
    }
}
