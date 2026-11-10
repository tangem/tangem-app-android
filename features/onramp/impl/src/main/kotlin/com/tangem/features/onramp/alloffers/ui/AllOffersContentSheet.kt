package com.tangem.features.onramp.alloffers.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.main.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.main.entity.OnrampOfferUM
import com.tangem.features.onramp.main.ui.OfferWithLinkedBanner
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun AllOffersContentSheet(
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
            if (state is AllOffersStateUM.Content && state.currentMethod != null) {
                ProviderTitle(
                    onCloseClick = onCloseClick,
                    onBackClick = onBack,
                )
            } else {
                PaymentMethodTitle(onCloseClick = onCloseClick)
            }
        },
        content = {
            AllOffersSheetContent(
                state = state,
                marketingBannerComponent = marketingBannerComponent,
                linkedMarketingBannerComponent = linkedMarketingBannerComponent,
            )
        },
    )
}

@Composable
private fun AllOffersSheetContent(
    state: AllOffersStateUM,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Standalone marketing banner at the top of the sheet (DS3 -> wrap in the redesign theme).
        // Renders nothing when no matching campaign, so it adds no space in the common case.
        IntrinsicFreeBox {
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
                    AllOffersStateUM.Loading -> AllOffersContentLoading()
                    is AllOffersStateUM.Error -> AllOffersError(state.errorNotification)
                    is AllOffersStateUM.Content -> {
                        if (shouldShowOffersScreen) {
                            state.currentMethod?.let { method ->
                                OffersBasedOnPaymentMethodContent(
                                    offers = method.offers,
                                    linkedMarketingBannerComponent = linkedMarketingBannerComponent,
                                    notification = state.restrictedNotification,
                                )
                            }
                        } else {
                            PaymentMethodsContent(
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
@Composable
private fun IntrinsicFreeBox(content: @Composable () -> Unit) {
    Layout(content = content, measurePolicy = IntrinsicFreeMeasurePolicy)
}

private object IntrinsicFreeMeasurePolicy : MeasurePolicy {

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

@Composable
private fun ProviderTitle(onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    TangemModal.TopNavigation(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.express_choose_providers_title),
        onBack = onBackClick,
        onClose = onCloseClick,
    )
}

@Composable
private fun PaymentMethodTitle(onCloseClick: () -> Unit) {
    TangemModal.TopNavigation(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.onramp_payment_method_subtitle),
        onClose = onCloseClick,
    )
}

@Composable
private fun OffersBasedOnPaymentMethodContent(
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

@Composable
private fun AllOffersContentLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(times = LOADING_ITEMS_COUNT) {
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

private const val LOADING_ITEMS_COUNT = 3

@Composable
private fun AllOffersError(errorNotification: NotificationUM) {
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
@Composable
private fun AllOffersContentSheetPaymentPreview() {
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
        AllOffersContentSheet(
            state = AllOffersStateUM.Content(
                methods = persistentListOf(),
                currentMethod = method,
                onBackClicked = {},
            ),
            marketingBannerComponent = PreviewMarketingBannerComponent,
            linkedMarketingBannerComponent = PreviewMarketingBannerComponent,
            onCloseClick = {},
        )
    }
}

private val PreviewMarketingBannerComponent = object : MarketingBannerComponent {
    @Composable
    override fun Content(modifier: Modifier) = Unit
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AllOffersContentSheetOffersPreview() {
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
        AllOffersContentSheet(
            state = AllOffersStateUM.Content(
                methods = methods.toPersistentList(),
                currentMethod = null,
                onBackClicked = {},
            ),
            marketingBannerComponent = PreviewMarketingBannerComponent,
            linkedMarketingBannerComponent = PreviewMarketingBannerComponent,
            onCloseClick = {},
        )
    }
}