package com.tangem.features.welcome.impl.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.userwallet.CardImage
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.welcome.impl.R
import com.tangem.features.welcome.impl.ui.state.WalletUM
import com.tangem.features.welcome.impl.ui.state.WelcomeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

@Suppress("MagicNumber")
@Composable
internal fun AnimatedContentScope.WelcomeSelectWallet(state: WelcomeUM.SelectWallet, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TopBar(state)
        TitleText()
        SpacerH12()

        var actualWallets by remember { mutableStateOf<ImmutableList<WalletUM>>(persistentListOf()) }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            tween(delayMillis = 300),
                            initialOffsetY = { it / 10 },
                        ) + fadeIn(tween(delayMillis = 300)),
                        exit = fadeOut(),
                    )
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(actualWallets) { index, walletState ->
                    WalletItem(
                        state = walletState,
                        modifier = Modifier,
                    )
                }
            }

            BottomFade(modifier = Modifier.align(Alignment.BottomCenter))

            SecondaryButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .animateEnterExit(fadeIn(), fadeOut()),
                text = "Unlock all with biometric",
                onClick = state.onUnlockWithBiometricClick,
            )
        }

        LaunchedEffect(state.wallets) {
            delay(200)
            actualWallets = state.wallets
        }
    }

    AddWalletBottomSheet(state.addWalletBottomSheet)
}

@Suppress("MagicNumber")
@Composable
private fun AnimatedContentScope.TopBar(state: WelcomeUM.SelectWallet, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .animateEnterExit(
                enter = slideInVertically(
                    tween(delayMillis = 100),
                    initialOffsetY = { it + 100 },
                ) + fadeIn(tween(delayMillis = 100)),
                exit = fadeOut(),
            )
            .padding(
                top = 16.dp,
                bottom = 16.dp,
                start = 16.dp,
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.img_tangem_logo_90_24),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )

        TextButton(
            modifier = Modifier.clip(TangemTheme.shapes.roundedCornersLarge),
            text = "Add Wallet",
            colors = TangemButtonsDefaults.defaultTextButtonColors.copy(
                contentColor = TangemTheme.colors.text.primary1,
            ),
            textStyle = TangemTheme.typography.body1,
            onClick = state.addWalletClick,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun AnimatedContentScope.TitleText(modifier: Modifier = Modifier) {
    Column(
        modifier.padding(16.dp),
    ) {
        Text(
            modifier = Modifier.animateEnterExit(
                enter = slideInVertically(
                    tween(delayMillis = 300),
                    initialOffsetY = { it + 200 },
                ) + fadeIn(tween(delayMillis = 300)),
                exit = fadeOut(),
            ),
            text = "Welcome back!",
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )

        SpacerH16()

        Text(
            modifier = Modifier.animateEnterExit(
                enter = slideInVertically(
                    tween(delayMillis = 300),
                    initialOffsetY = { it + 200 },
                ) + fadeIn(tween(delayMillis = 300)),
                exit = fadeOut(),
            ),
            text = "Select a wallet to log in",
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun WalletItem(state: WalletUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.secondary, TangemTheme.shapes.roundedCornersXMedium)
            .clickable(onClick = state.onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletImage(state.imageState)

        SpacerW12()

        Column(Modifier.weight(1f)) {
            Text(
                text = state.name.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )

            Text(
                text = state.subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun WalletImage(state: WalletUM.ImageState, modifier: Modifier = Modifier) {
    when (state) {
        WalletUM.ImageState.MobileWallet -> {
            Box(
                modifier = modifier
                    .size(36.dp)
                    .background(TangemTheme.colors.icon.accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_wallet_filled_24),
                    tint = TangemTheme.colors.icon.accent,
                    contentDescription = null,
                )
            }
        }
        else -> {
            CardImage(
                imageState = when (state) {
                    is WalletUM.ImageState.Image -> UserWalletItemUM.ImageState.Image(state.artwork)
                    WalletUM.ImageState.Loading -> UserWalletItemUM.ImageState.Loading
                    else -> error("")
                },
                modifier = modifier,
            )
        }
    }
}