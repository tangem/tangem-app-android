package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarActionContent
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.onramp.featuretoggles.LocalOnrampDesignSystemEnabled
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampCountryButtonUM
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampMainTopBarUM
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun OnrampMainScreen(
    state: OnrampMainComponentUM,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
    ctaMarketingBannerComponent: MarketingBannerComponent,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = { OnrampTopBar(config = state.topBarConfig) },
        contentWindowInsets = WindowInsetsZero,
        containerColor = TangemTheme.colors3.bg.primary,
    ) { scaffoldPaddings ->
        OnrampMainComponentContent(
            state = state,
            marketingBannerComponent = marketingBannerComponent,
            linkedMarketingBannerComponent = linkedMarketingBannerComponent,
            ctaMarketingBannerComponent = ctaMarketingBannerComponent,
            modifier = Modifier.padding(scaffoldPaddings),
        )
    }
}

@Composable
private fun OnrampTopBar(config: OnrampMainTopBarUM, modifier: Modifier = Modifier) {
    val countryButton = config.countryButtonUM
    if (countryButton != null) {
        TangemTopBar(
            modifier = modifier,
            title = config.title,
            subtitle = config.subtitle,
            startContent = { CountryButton(state = countryButton) },
            endContent = { CloseButton(state = config.startButtonUM) },
        )
    } else {
        TangemTopAppBar(
            modifier = modifier,
            startButton = config.startButtonUM,
            endButton = config.endButtonUM,
            title = config.title.resolveReference(),
            subtitle = config.subtitle.resolveReference(),
        )
    }
}

@Composable
private fun CountryButton(state: OnrampCountryButtonUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                enabled = state.isEnabled,
                role = Role.Button,
                onClickLabel = stringResourceSafe(R.string.onramp_settings_residence),
                onClick = state.onClick,
            )
            .semantics { state.countryName?.let { contentDescription = it } }
            .testTag(TopAppBarTestTags.MORE_BUTTON),
        contentAlignment = Alignment.Center,
    ) {
        if (state.flagUrl != null) {
            SubcomposeAsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = state.flagUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { CircleShimmer(modifier = Modifier.fillMaxSize()) },
                error = {
                    Box(Modifier.fillMaxSize().background(TangemTheme.colors.stroke.primary, CircleShape))
                },
            )
        }
    }
}

@Composable
private fun CloseButton(state: TopAppBarButtonUM, modifier: Modifier = Modifier) {
    TangemTopBarActionContent(
        actionUM = TangemTopBarActionUM(
            iconRes = CoreUiR.drawable.ic_close_24,
            isActionable = state.isEnabled,
            onClick = state.onClicked,
            ghostModeProgress = 1f,
        ),
        modifier = modifier.testTag(TopAppBarTestTags.CLOSE_BUTTON),
    )
}

@Composable
internal fun OnrampMainComponentContent(
    state: OnrampMainComponentUM,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
    ctaMarketingBannerComponent: MarketingBannerComponent,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is OnrampMainComponentUM.InitialLoading -> InitialLoading(state = state)
                is OnrampMainComponentUM.Content -> Content(
                    state = state,
                    marketingBannerComponent = marketingBannerComponent,
                    linkedMarketingBannerComponent = linkedMarketingBannerComponent,
                    ctaMarketingBannerComponent = ctaMarketingBannerComponent,
                )
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OnrampAmountContentLoading()
        OnrampNotifications(state = state)
    }
}

@Composable
private fun OnrampAmountContentLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 16.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RectangleShimmer(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(width = 76.dp, height = 20.dp),
            radius = 4.dp,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 136.dp, height = 44.dp),
            radius = 4.dp,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = 52.dp, height = 16.dp),
            radius = 4.dp,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = 20.dp)
                .size(width = 84.dp, height = 28.dp),
            radius = 14.dp,
        )
    }
}

@Composable
private fun Content(
    state: OnrampMainComponentUM.Content,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
    ctaMarketingBannerComponent: MarketingBannerComponent,
    modifier: Modifier = Modifier,
) {
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (LocalOnrampDesignSystemEnabled.current) {
            OnrampAmountContentDs(state = state)
        } else {
            OnrampAmountContent(state = state)
        }

        TangemThemeRedesign {
            marketingBannerComponent.Content(Modifier.fillMaxWidth())
            ctaMarketingBannerComponent.Content(Modifier.fillMaxWidth())
        }

        OnrampOffersContent(
            state = state.offersBlockState,
            linkedMarketingBannerComponent = linkedMarketingBannerComponent,
        )

        OnrampNotifications(state = state)
    }
}

@Composable
private fun OnrampNotifications(state: OnrampMainComponentUM, modifier: Modifier = Modifier) {
    val buyNotSupportedMessage = state.buyNotSupportedMessage
    val errorNotification = state.errorNotification
    when {
        buyNotSupportedMessage != null -> if (LocalOnrampDesignSystemEnabled.current) {
            BuyNotSupportedBannerDs(messageUM = buyNotSupportedMessage, modifier = modifier)
        } else {
            TangemMessage(messageUM = buyNotSupportedMessage, modifier = modifier)
        }
        errorNotification != null -> Notification(config = errorNotification.config, modifier = modifier)
    }
}