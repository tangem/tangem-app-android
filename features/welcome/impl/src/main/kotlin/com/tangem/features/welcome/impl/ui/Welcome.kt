package com.tangem.features.welcome.impl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.welcome.impl.ui.state.WelcomeUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun Welcome(state: WelcomeUM, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class.java.simpleName },
        ) { st ->
            when (st) {
                is WelcomeUM.Plain -> WelcomePlain(modifier = modifier)
                is WelcomeUM.SelectWallet -> WelcomeSelectWallet(
                    state = st,
                    modifier = modifier,
                )
                WelcomeUM.Empty -> {}
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        val state = WelcomeUM.SelectWallet(
            wallets = persistentListOf(
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_3".encodeToByteArray()),
                    name = stringReference("Multi Card"),
                    information = UserWalletItemUM.Information.Loading,
                    balance = UserWalletItemUM.Balance.Loaded(
                        value = "1.2345 BTC",
                        isFlickering = false,
                    ),
                    isEnabled = true,
                    onClick = {},
                ),
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_3".encodeToByteArray()),
                    name = stringReference("Multi Card"),
                    information = UserWalletItemUM.Information.Failed,
                    imageState = UserWalletItemUM.ImageState.MobileWallet,
                    balance = UserWalletItemUM.Balance.Locked,
                    isEnabled = true,
                    onClick = {},
                ),
            ),
        )

        var currentState by remember { mutableStateOf<WelcomeUM>(WelcomeUM.SelectWallet()) }

        Box {
            Welcome(currentState)

            Button(
                modifier = Modifier.align(Alignment.BottomStart),
                onClick = {
                    currentState = when (currentState) {
                        is WelcomeUM.Plain -> state
                        is WelcomeUM.SelectWallet -> WelcomeUM.Empty
                        WelcomeUM.Empty -> WelcomeUM.Plain
                    }
                },
            ) {
                Text("Switch")
            }
        }
    }
}