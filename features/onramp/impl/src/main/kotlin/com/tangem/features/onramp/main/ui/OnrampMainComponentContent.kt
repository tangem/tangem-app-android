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
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.onramp.featuretoggles.LocalOnrampDesignSystemEnabled
import com.tangem.features.onramp.main.entity.OnrampCountryButtonUM
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampMainTopBarUM
import com.tangem.features.onramp.main.entity.takesSlots

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
private fun OnrampTopNavigationDs(
    config: OnrampMainTopBarUM,
    countryButton: OnrampCountryButtonUM,
    modifier: Modifier = Modifier,
) {
    TangemTopNavigation(
        modifier = modifier,
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        blurBackground = false,
        startButton = { CountryButtonDs(state = countryButton) },
        endButton = { CloseButtonDs(state = config.startButtonUM) },
        contentColumn = {
            TangemNavigationText(text = config.title, role = TangemNavigationText.Role.Title)
            SpacerH(4.dp)
            TangemNavigationText(text = config.subtitle, role = TangemNavigationText.Role.Subtitle)
        },
    )
}

@Composable
private fun OnrampTopBar(config: OnrampMainTopBarUM, modifier: Modifier = Modifier) {
    val countryButton = config.countryButtonUM
    if (countryButton != null) {
        OnrampTopNavigationDs(config = config, countryButton = countryButton, modifier = modifier)
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
        RegionRestrictionBanner(state)
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
        verticalArrangement = Arrangement.spacedBy(if (LocalOnrampDesignSystemEnabled.current) 8.dp else 12.dp),
    ) {
        if (LocalOnrampDesignSystemEnabled.current) {
            OnrampAmountContentDs(state = state)
        } else {
            OnrampAmountContent(state = state)
        }
        RegionRestrictionBanner(state)

        TangemThemeRedesign {
            marketingBannerComponent.Content(Modifier.fillMaxWidth())
            ctaMarketingBannerComponent.CtaContent(slot = null, modifier = Modifier.fillMaxWidth())
            if (!state.offersBlockState.takesSlots()) {
                ctaMarketingBannerComponent.replacedSlots().forEach { slot ->
                    ctaMarketingBannerComponent.CtaContent(slot = slot, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        OnrampOffersContent(
            state = state.offersBlockState,
            ctaMarketingBannerComponent = ctaMarketingBannerComponent,
            linkedMarketingBannerComponent = linkedMarketingBannerComponent,
        )

        OnrampNotifications(state = state)
    }
}

@Composable
private fun OnrampNotifications(state: OnrampMainComponentUM, modifier: Modifier = Modifier) {
    val buyNotSupportedMessage = state.buyNotSupportedMessage
    val errorNotification = state.errorNotification
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (buyNotSupportedMessage != null) {
            if (LocalOnrampDesignSystemEnabled.current) {
                BuyNotSupportedBannerDs(messageUM = buyNotSupportedMessage)
            } else {
                TangemMessage(messageUM = buyNotSupportedMessage)
            }
        }
        if (errorNotification != null) {
            Notification(config = errorNotification.config)
        }
    }
}

@Composable
private fun RegionRestrictionBanner(state: OnrampMainComponentUM) {
    val banner = state.regionRestrictionMessage ?: return
    TangemThemeRedesign {
        TangemMessageBanner(state = banner, modifier = Modifier.fillMaxWidth())
    }
}