package com.tangem.tap.features.details.ui.walletconnect

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WalletConnectScreen(
    state: WalletConnectScreenState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreensScaffold(
        modifier = modifier,
        content = {
            if (state.sessions.isEmpty()) {
                EmptyScreen(state)
            } else {
                WalletConnectSessions(state)
            }
        },
        fab = {
            if (!state.isLoading) {
                AddSessionFab(
                    onAddSession = {
                        Analytics.send(Settings.ButtonStartWalletConnectSession())
                        state.onAddSession()
                    },
                )
            }
        },
        titleRes = R.string.wallet_connect_title,
        onBackClick = onBackClick,
    )
}

@Composable
private fun AddSessionFab(onAddSession: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onAddSession,
        containerColor = TangemTheme.colors.button.primary,
        contentColor = TangemTheme.colors.icon.primary2,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_plus_24),
            contentDescription = "",
        )
    }
}

@Composable
private fun EmptyScreen(state: WalletConnectScreenState) {
    if (state.isLoading) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = TangemTheme.colors.icon.accent,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_wallet_connect_24),
            contentDescription = "",
            colorFilter = ColorFilter.tint(TangemTheme.colors.icon.inactive),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.width(width = 100.dp),
        )
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = stringResourceSafe(id = R.string.wallet_connect_subtitle),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun WalletConnectSessions(state: WalletConnectScreenState) {
    if (state.isLoading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = TangemTheme.colors.icon.accent,
        )
    } else {
        Spacer(modifier = Modifier.height(2.dp))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(state.sessions) { session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = session.description,
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        Analytics.send(Settings.ButtonStopWalletConnectSession())
                        state.onRemoveSession(session.sessionId)
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cross_rounded_24),
                        contentDescription = "",
                        tint = TangemTheme.colors.icon.warning,
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun WalletConnectScreenPreview() {
    WalletConnectScreen(
        state = WalletConnectScreenState(
            sessions = persistentListOf(
                WcSessionForScreen(
                    description = "session from some dApp",
                    sessionId = "",
                ),
            ),
            isLoading = true,
        ),
        {},
    )
}