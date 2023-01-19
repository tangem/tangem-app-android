package com.tangem.tap.features.details.ui.walletconnect

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun WalletConnectScreen(state: WalletConnectScreenState, onBackClick: () -> Unit) {
    val context = LocalContext.current

    SettingsScreensScaffold(
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
                        state.onAddSession(context.getFromClipboard()?.toString())
                    },
                )
            }
        },
        titleRes = R.string.wallet_connect_title,
        onBackClick = onBackClick,
    )
}

@Composable
private fun AddSessionFab(
    onAddSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onAddSession,
        backgroundColor = colorResource(id = R.color.button_primary),
        contentColor = colorResource(id = R.color.icon_primary_2),
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
            color = colorResource(id = R.color.icon_accent),
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
            painter = painterResource(id = R.drawable.ic_walletconnect),
            contentDescription = "",
            colorFilter = ColorFilter.tint(colorResource(id = R.color.icon_inactive)),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.width(width = 100.dp),
        )
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = stringResource(id = R.string.wallet_connect_subtitle),
            style = TangemTypography.body2,
            color = colorResource(id = R.color.text_tertiary),
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
            color = colorResource(id = R.color.icon_accent),
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
                    style = TangemTypography.subtitle1,
                    color = colorResource(id = R.color.text_primary_1),
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
                        tint = colorResource(id = R.color.icon_warning),
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
            sessions = listOf(
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
