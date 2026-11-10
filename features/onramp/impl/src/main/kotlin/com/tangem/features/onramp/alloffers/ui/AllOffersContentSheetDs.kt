package com.tangem.features.onramp.alloffers.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodStatus
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.onramp.alloffers.entity.AllOffersPaymentMethodUM
import com.tangem.features.onramp.alloffers.entity.AllOffersStateUM
import com.tangem.features.onramp.alloffers.entity.OnrampPaymentMethodConfig
import com.tangem.features.onramp.featuretoggles.LocalOnrampDesignSystemEnabled
import com.tangem.features.onramp.impl.R
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.main.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.main.entity.OnrampOfferUM
import com.tangem.features.onramp.main.ui.OfferWithLinkedBanner
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
internal fun AllOffersContentSheetDs(
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

    val config = TangemBottomSheetConfig(
        isShown = true,
        onDismissRequest = onCloseClick,
        content = TangemBottomSheetConfigContent.Empty,
    )
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = config,
        onBack = onBack,
        title = {
            CompositionLocalProvider(LocalOnrampDesignSystemEnabled provides true) {
                if (state is AllOffersStateUM.Content && state.currentMethod != null) {
                    ProviderTitleDs(
                        onCloseClick = onCloseClick,
                        onBackClick = onBack,
                    )
                } else {
                    PaymentMethodTitleDs(onCloseClick = onCloseClick)
                }
            }
        },
        content = {
            CompositionLocalProvider(LocalOnrampDesignSystemEnabled provides true) {
                AllOffersSheetContentDs(
                    state = state,
                    marketingBannerComponent = marketingBannerComponent,
                    linkedMarketingBannerComponent = linkedMarketingBannerComponent,
                )
            }
        },
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun AllOffersSheetContentDs(
    state: AllOffersStateUM,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Standalone marketing banner at the top of the sheet (DS3 -> wrap in the redesign theme).
        // Renders nothing when no matching campaign, so it adds no space in the common case.
        IntrinsicFreeBoxDs {
            TangemThemeRedesign {
                marketingBannerComponent.Content(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                    AllOffersStateUM.Loading -> AllOffersContentLoadingDs()
                    is AllOffersStateUM.Error -> AllOffersErrorDs(state.errorNotification)
                    is AllOffersStateUM.Content -> {
                        if (shouldShowOffersScreen) {
                            state.currentMethod?.let { method ->
                                OffersBasedOnPaymentMethodContentDs(
                                    offers = method.offers,
                                    linkedMarketingBannerComponent = linkedMarketingBannerComponent,
                                    notification = state.restrictedNotification,
                                )
                            }
                        } else {
                            PaymentMethodsContentDs(
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

// The banner can be a pager, which throws on the intrinsic-size queries TangemModal makes.
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun IntrinsicFreeBoxDs(content: @Composable () -> Unit) {
    Layout(content = content, measurePolicy = IntrinsicFreeMeasurePolicyDs)
}

private object IntrinsicFreeMeasurePolicyDs : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = placeables.sumOf { it.height }
        return layout(width, height) {
            var y = 0
            placeables.forEach {
                it.placeRelative(x = 0, y = y)
                y += it.height
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun ProviderTitleDs(onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    TangemModal.TopNavigation(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.express_choose_providers_title),
        onBack = onBackClick,
        onClose = onCloseClick,
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun PaymentMethodTitleDs(onCloseClick: () -> Unit) {
    TangemModal.TopNavigation(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.onramp_payment_method_subtitle),
        onClose = onCloseClick,
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun OffersBasedOnPaymentMethodContentDs(
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
            TangemMessage(config = notification.config)
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

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun AllOffersContentLoadingDs() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(times = LOADING_ITEMS_COUNT_DS) {
            ProvideTangemShimmer {
                TangemShimmer(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(96.dp),
                    radius = 14.dp,
                )
            }
        }
    }
}

private const val LOADING_ITEMS_COUNT_DS = 3

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun AllOffersErrorDs(errorNotification: NotificationUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        TangemMessage(config = errorNotification.config)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun AllOffersContentSheetPaymentPreviewDs() {
    val method = AllOffersPaymentMethodUM(
        offers = persistentListOf(
            OnrampOfferUM(
                category = OnrampOfferCategoryUM.Recommended,
                advantages = OnrampOfferAdvantagesUM.BestRate,
                paymentMethod = OnrampPaymentMethod(
                    id = "card",
                    name = "Card",
                    imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
                    type = PaymentMethodType.CARD,
                ),
                providerId = "simplex",
                providerName = "Simplex",
                rate = "0,0245334 BTC",
                diff = null,
                onBuyClicked = {},
            ),
            OnrampOfferUM(
                category = OnrampOfferCategoryUM.Recommended,
                advantages = OnrampOfferAdvantagesUM.Default,
                paymentMethod = OnrampPaymentMethod(
                    id = "card",
                    name = "Card",
                    imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
                    type = PaymentMethodType.CARD,
                ),
                providerId = "simplex",
                providerName = "Simplex",
                rate = "0,00145334 BTC",
                diff = stringReference("–0.07%"),
                onBuyClicked = {},
            ),
        ),
        methodConfig = OnrampPaymentMethodConfig(
            method = OnrampPaymentMethod(
                id = "card",
                name = "Card",
                imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
                type = PaymentMethodType.CARD,
            ),
            onClick = {},
        ),
        diff = null,
        rate = "0,0245334 BTC",
        providersCount = 2,
        isBestRate = true,
        paymentMethodStatus = PaymentMethodStatus.Available,
    )

    TangemThemePreview {
        AllOffersContentSheetDs(
            state = AllOffersStateUM.Content(
                methods = persistentListOf(),
                currentMethod = method,
                onBackClicked = {},
            ),
            marketingBannerComponent = PreviewMarketingBannerComponentDs,
            linkedMarketingBannerComponent = PreviewMarketingBannerComponentDs,
            onCloseClick = {},
        )
    }
}

private val PreviewMarketingBannerComponentDs = object : MarketingBannerComponent {
    @Composable
    override fun Content(modifier: Modifier) = Unit
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun AllOffersContentSheetOffersPreviewDs() {
    val methods = List(5) {
        AllOffersPaymentMethodUM(
            offers = persistentListOf(
                OnrampOfferUM(
                    category = OnrampOfferCategoryUM.Recommended,
                    advantages = OnrampOfferAdvantagesUM.BestRate,
                    paymentMethod = OnrampPaymentMethod(
                        id = "card",
                        name = "Card",
                        imageUrl =
                        "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
                        type = PaymentMethodType.CARD,
                    ),
                    providerId = "simplex",
                    providerName = "Simplex",
                    rate = "0,0245334 BTC",
                    diff = null,
                    onBuyClicked = {},
                ),
                OnrampOfferUM(
                    category = OnrampOfferCategoryUM.Recommended,
                    advantages = OnrampOfferAdvantagesUM.Default,
                    paymentMethod = OnrampPaymentMethod(
                        id = "card",
                        name = "Card",
                        imageUrl =
                        "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
                        type = PaymentMethodType.CARD,
                    ),
                    providerId = "simplex",
                    providerName = "Simplex",
                    rate = "0,00145334 BTC",
                    diff = stringReference("–0.07%"),
                    onBuyClicked = {},
                ),
            ),
            methodConfig = OnrampPaymentMethodConfig(
                method = OnrampPaymentMethod(
                    id = "card",
                    name = "Card",
                    imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
                    type = PaymentMethodType.CARD,
                ),
                onClick = {},
            ),
            diff = null,
            rate = "0,0245334 BTC",
            providersCount = 2,
            isBestRate = true,
            paymentMethodStatus = PaymentMethodStatus.Available,
        )
    }

    TangemThemePreview {
        AllOffersContentSheetDs(
            state = AllOffersStateUM.Content(
                methods = methods.toPersistentList(),
                currentMethod = null,
                onBackClicked = {},
            ),
            marketingBannerComponent = PreviewMarketingBannerComponentDs,
            linkedMarketingBannerComponent = PreviewMarketingBannerComponentDs,
            onCloseClick = {},
        )
    }
}