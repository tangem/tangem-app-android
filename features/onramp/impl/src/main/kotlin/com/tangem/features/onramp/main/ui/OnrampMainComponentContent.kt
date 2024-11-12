package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM

@Composable
internal fun OnrampMainComponentContent(state: OnrampMainComponentUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsetsZero,
        containerColor = TangemTheme.colors.background.secondary,
        topBar = {
            TangemTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                startButton = state.topBarConfig.startButtonUM,
                endButton = state.topBarConfig.endButtonUM,
                title = state.topBarConfig.title.resolveReference(),
            )
        },
        content = { innerPadding ->
            val contentModifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth()
                .wrapContentHeight()
            when (state) {
                is OnrampMainComponentUM.InitialLoading -> InitialLoading(modifier = contentModifier)
                is OnrampMainComponentUM.Content -> Content(modifier = contentModifier)
            }
        },
        floatingActionButton = {
            BuyButton(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                onClick = state.buyButtonConfig.onClick,
                enabled = state.buyButtonConfig.enabled,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    )
}

@Composable
private fun InitialLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = TangemTheme.dimens.radius16))
            .background(TangemTheme.colors.background.action)
            .padding(vertical = TangemTheme.dimens.spacing28),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircleShimmer(modifier = Modifier.size(TangemTheme.dimens.size40))
        RectangleShimmer(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing16)
                .size(width = TangemTheme.dimens.size96, height = TangemTheme.dimens.size24),
            radius = TangemTheme.dimens.radius3,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing16)
                .size(width = TangemTheme.dimens.size72, height = TangemTheme.dimens.size12),
            radius = TangemTheme.dimens.radius3,
        )
    }
}

@Composable
private fun Content(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

@Composable
private fun BuyButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    PrimaryButton(
        modifier = modifier,
        text = stringResource(id = R.string.common_buy),
        onClick = onClick,
        enabled = enabled,
    )
}
