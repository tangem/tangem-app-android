package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM

@Composable
internal fun OnrampMainScreen(state: OnrampMainComponentUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            TangemTopAppBar(
                startButton = state.topBarConfig.startButtonUM,
                endButton = state.topBarConfig.endButtonUM,
                title = state.topBarConfig.title.resolveReference(),
            )
        },
        contentWindowInsets = WindowInsetsZero,
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        OnrampMainComponentContent(
            state = state,
            modifier = Modifier.padding(scaffoldPaddings),
        )
    }
}

@Composable
internal fun OnrampMainComponentContent(state: OnrampMainComponentUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is OnrampMainComponentUM.InitialLoading -> InitialLoading(state = state)
                is OnrampMainComponentUM.Content -> Content(state = state)
            }
        }

        if (state is OnrampMainComponentUM.Content) {
            OnrampFooterContent(state = state)
        }
    }
}

@Composable
private fun InitialLoading(state: OnrampMainComponentUM.InitialLoading, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        OnrampAmountContentLoading()
        if (state.errorNotification != null) Notification(config = state.errorNotification.config)
    }
}

@Composable
private fun OnrampAmountContentLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = TangemTheme.dimens.radius16))
            .background(TangemTheme.colors.background.action)
            .padding(vertical = TangemTheme.dimens.spacing28),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RectangleShimmer(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing16)
                .size(width = 76.dp, height = 20.dp),
            radius = TangemTheme.dimens.radius4,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing12)
                .size(width = 136.dp, height = 44.dp),
            radius = TangemTheme.dimens.radius4,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing8)
                .size(width = 52.dp, height = 16.dp),
            radius = TangemTheme.dimens.radius4,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing20)
                .size(width = 84.dp, height = 28.dp),
            radius = TangemTheme.dimens.radius14,
        )
    }
}

@Composable
private fun Content(state: OnrampMainComponentUM.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .padding(
                bottom = 76.dp,
                start = 16.dp,
                end = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        OnrampAmountContent(state = state)

        OnrampOffersContent(state = state.offersBlockState)

        if (state.errorNotification != null) Notification(config = state.errorNotification.config)
    }
}