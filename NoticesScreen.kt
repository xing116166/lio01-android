package com.xboard.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xboard.app.AppState
import com.xboard.app.data.Notice
import com.xboard.app.ui.components.AppTopBar
import com.xboard.app.ui.components.EmptyState

@Composable
fun NoticesScreen(
    state: AppState,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf<Notice?>(null) }

    Scaffold(
        topBar = { AppTopBar(title = "公告", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.notices.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Campaign,
                        title = "暂无公告",
                        subtitle = "面板发布公告后会显示在这里",
                    )
                }
            } else {
                items(state.notices, key = { it.id }) { notice ->
                    NoticeRow(notice = notice, onClick = { selected = notice })
                }
            }
        }
    }

    selected?.let { notice ->
        NoticeDialog(notice = notice, onDismiss = { selected = null })
    }
}
