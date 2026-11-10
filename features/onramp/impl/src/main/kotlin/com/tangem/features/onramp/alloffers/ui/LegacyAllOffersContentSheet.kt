package com.tangem.features.onramp.alloffers.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.onramp.alloffers.entity.AllOffersStateUM
import com.tangem.features.onramp.impl.R
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.features.onramp.main.entity.OnrampOfferUM
import com.tangem.features.onramp.main.ui.OfferWithLinkedBanner
import kotlinx.collections.immutable.ImmutableList

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED")
@Composable
internal fun LegacyAllOffersContentSheet(
    state: AllOffersStateUM,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
    onCloseClick: () -> Unit,
) {
    val onBack = remember(state) {
        {
            if (state is AllOffersStateUM.Content && state.currentMethod != null) {
                state.onBackClicked()
            } else {
                onCloseClick()
            }
        }
    }

    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onCloseClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBack,
        containerColor = TangemTheme.colors3.bg.primary,
        title = {
            if (state is AllOffersStateUM.Content && state.currentMethod != null) {
                LegacyProviderTitle(
                    onCloseClick = onCloseClick,
                    onBackClick = onBack,
                )
            } else {
                LegacyPaymentMethodTitle(onCloseClick = onCloseClick)
            }
        },
        content = {
            LegacyAllOffersSheetContent(
                state = state,
                marketingBannerComponent = marketingBannerComponent,
                linkedMarketingBannerComponent = linkedMarketingBannerComponent,
            )
        },
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED")
@Composable
private fun LegacyAllOffersSheetContent(
    state: AllOffersStateUM,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Standalone marketing banner at the top of the sheet (DS3 -> wrap in the redesign theme).
        // Renders nothing when no matching campaign, so it adds no space in the common case.
        TangemThemeRedesign {
            marketingBannerComponent.Content(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
        ) {
            AnimatedContent(
                targetState = state is AllOffersStateUM.Content && state.currentMethod != null,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 220)) togetherWith
                        fadeOut(tween(durationMillis = 90))
                },
                label = "Change offers and payment method state",
            ) { shouldShowOffersScreen ->
                when (state) {
                    AllOffersStateUM.Loading -> LegacyAllOffersContentLoading()
                    is AllOffersStateUM.Error -> LegacyAllOffersError(state.errorNotification)
                    is AllOffersStateUM.Content -> {
                        if (shouldShowOffersScreen) {
                            state.currentMethod?.let { method ->
                                LegacyOffersBasedOnPaymentMethodContent(
                                    offers = method.offers,
                                    linkedMarketingBannerComponent = linkedMarketingBannerComponent,
                                    notification = state.restrictedNotification,
                                )
                            }
                        } else {
                            LegacyPaymentMethodsContent(
                                methods = state.methods,
                                notification = state.restrictedNotification,
                            )
                        }
                    }
                }
            }
        }
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED")
@Composable
private fun LegacyProviderTitle(onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    TangemModalBottomSheetTitle(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.express_choose_providers_title),
        startIconRes = R.drawable.ic_back_24,
        onStartClick = onBackClick,
        endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
        onEndClick = onCloseClick,
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED")
@Composable
private fun LegacyPaymentMethodTitle(onCloseClick: () -> Unit) {
    TangemModalBottomSheetTitle(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.onramp_payment_method_subtitle),
        endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
        onEndClick = onCloseClick,
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED")
@Composable
private fun LegacyOffersBasedOnPaymentMethodContent(
    offers: ImmutableList<OnrampOfferUM>,
    linkedMarketingBannerComponent: MarketingBannerComponent,
    notification: NotificationUM?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (notification != null) {
            Notification(config = notification.config)
            SpacerH(8.dp)
        }
        offers.fastForEach { offer ->
            key("${offer.paymentMethod.id} ${offer.providerName} ${offer.rate}") {
                OfferWithLinkedBanner(offer, linkedMarketingBannerComponent)
                SpacerH(8.dp)
            }
        }
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED")
@Composable
private fun LegacyAllOffersContentLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.primary)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RectangleShimmer(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(96.dp)
                .size(width = 76.dp, height = 20.dp),
            radius = 14.dp,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(96.dp)
                .size(width = 76.dp, height = 20.dp),
            radius = 14.dp,
        )
        RectangleShimmer(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(96.dp)
                .size(width = 76.dp, height = 20.dp),
            radius = 14.dp,
        )
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED")
@Composable
fun LegacyAllOffersError(errorNotification: NotificationUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Notification(config = errorNotification.config)
    }
}