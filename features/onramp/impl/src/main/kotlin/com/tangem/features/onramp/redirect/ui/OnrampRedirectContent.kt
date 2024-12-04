package com.tangem.features.onramp.redirect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.redirect.entity.OnrampRedirectUM

@Composable
internal fun OnrampRedirectContent(state: OnrampRedirectUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.imePadding(),
        contentWindowInsets = WindowInsetsZero,
        containerColor = TangemTheme.colors.background.secondary,
        topBar = {
            TangemTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                startButton = state.topBarConfig.startButtonUM,
                title = state.topBarConfig.title.resolveReference(),
            )
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = TangemTheme.dimens.spacing16),
                state = state,
            )
        },
    )
}

@Composable
private fun Content(state: OnrampRedirectUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LogoBlock(providerImageUrl = state.providerImageUrl)
        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing24),
            text = state.title.resolveReference(),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
            text = state.subtitle.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LogoBlock(providerImageUrl: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens.size64)
                .clip(TangemTheme.shapes.roundedCorners8)
                .background(TangemTheme.colors.background.primary)
                .padding(vertical = TangemTheme.dimens.spacing8, horizontal = TangemTheme.dimens.spacing12),
            painter = painterResource(id = R.drawable.ic_tangem_24),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
        LoadingDots()
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size64)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(providerImageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            contentDescription = null,
        )
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.size6)
                .clip(CircleShape)
                .background(color = TangemTheme.colors.icon.accent.copy(alpha = 0.75F)),
        )
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.size6)
                .clip(CircleShape)
                .background(color = TangemTheme.colors.icon.accent.copy(alpha = 0.45F)),
        )
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.size8)
                .clip(CircleShape)
                .background(color = TangemTheme.colors.icon.accent),
        )
    }
}