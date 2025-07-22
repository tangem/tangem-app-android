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
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.welcome.impl.ui.state.WalletUM
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
                is WelcomeUM.EnterAccessCode -> WelcomeEnterAccessCode(
                    state = st,
                    modifier = modifier,
                )
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
                WalletUM(
                    name = TextReference.Str("Wallet 1"),
                    subtitle = TextReference.Str("3 cards"),
                    imageState = WalletUM.ImageState.Loading,
                    onClick = {},
                ),
                WalletUM(
                    name = TextReference.Str("Wallet 1"),
                    subtitle = TextReference.Str("Mobile wallet"),
                    imageState = WalletUM.ImageState.MobileWallet,
                    onClick = {},
                ),
            ),
        )

        var currentState by remember { mutableStateOf<WelcomeUM>(WelcomeUM.EnterAccessCode()) }

        Box {
            Welcome(currentState)

            Button(
                modifier = Modifier.align(Alignment.BottomStart),
                onClick = {
                    currentState = when (currentState) {
                        is WelcomeUM.Plain -> state
                        is WelcomeUM.SelectWallet -> WelcomeUM.EnterAccessCode(
                            value = "",
                            onValueChange = {},
                        )
                        is WelcomeUM.EnterAccessCode -> WelcomeUM.Plain
                    }
                },
            ) {
                Text("Switch")
            }
        }
    }
}