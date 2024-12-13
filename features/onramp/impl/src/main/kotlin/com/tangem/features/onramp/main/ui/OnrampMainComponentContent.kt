package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM

@Composable
internal fun OnrampMainComponentContent(state: OnrampMainComponentUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.imePadding(),
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
                is OnrampMainComponentUM.InitialLoading -> InitialLoading(modifier = contentModifier, state = state)
                is OnrampMainComponentUM.Content -> Content(modifier = contentModifier, state = state)
            }
        },
        floatingActionButton = {
            OnrampButtonComponent(state)
        },
        floatingActionButtonPosition = FabPosition.Center,
    )
}

@Composable
private fun InitialLoading(state: OnrampMainComponentUM.InitialLoading, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        OnrampAmountContentLoading()
        if (state.errorNotification != null) Notification(config = state.errorNotification.config)
    }
}

@Composable
private fun OnrampAmountContentLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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
private fun Content(state: OnrampMainComponentUM.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = TangemTheme.dimens.spacing76),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        OnrampAmountContent(state = state.amountBlockState)
        OnrampProviderContent(state = state.providerBlockState, modifier = Modifier.fillMaxWidth())
        if (state.errorNotification != null) Notification(config = state.errorNotification.config)
    }
}