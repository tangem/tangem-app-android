package com.tangem.features.onramp.alloffers.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
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
    val isNewDesignSystemEnabled = LocalOnrampDesignSystemEnabled.current
    val title: @Composable () -> Unit = {
        CompositionLocalProvider(LocalOnrampDesignSystemEnabled provides isNewDesignSystemEnabled) {
            if (state is AllOffersStateUM.Content && state.currentMethod != null) {
                ProviderTitle(
                    onCloseClick = onCloseClick,
                    onBackClick = onBack,
                )
            } else {
                PaymentMethodTitle(onCloseClick = onCloseClick)
            }
        }
    }
    val content: @Composable () -> Unit = {
        CompositionLocalProvider(LocalOnrampDesignSystemEnabled provides isNewDesignSystemEnabled) {
            AllOffersSheetContent(
                state = state,
                marketingBannerComponent = marketingBannerComponent,
                linkedMarketingBannerComponent = linkedMarketingBannerComponent,
            )
        }
    }

    if (isNewDesignSystemEnabled) {
        TangemModal<TangemBottomSheetConfigContent.Empty>(
            config = config,
            onBack = onBack,
            title = { title() },
            content = { content() },
        )
    } else {
        TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = config,
            onBack = onBack,
            containerColor = TangemTheme.colors3.bg.primary,
            title = { title() },
            content = { content() },
        )
    }
}

@Composable
private fun AllOffersSheetContent(
    state: AllOffersStateUM,
    marketingBannerComponent: MarketingBannerComponent,
    linkedMarketingBannerComponent: MarketingBannerComponent,
) {
    val isNewDesignSystemEnabled = LocalOnrampDesignSystemEnabled.current
    Column(modifier = if (isNewDesignSystemEnabled) Modifier.fillMaxWidth() else Modifier.fillMaxSize()) {
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
            modifier = (if (isNewDesignSystemEnabled) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
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
    if (LocalOnrampDesignSystemEnabled.current) {
        TangemModal.TopNavigation(
            title = TextReference.Res(R.string.onramp_all_offers_button_title),
            subtitle = TextReference.Res(R.string.express_choose_providers_title),
            onBack = onBackClick,
            onClose = onCloseClick,
        )
    } else {
        TangemModalBottomSheetTitle(
            title = TextReference.Res(R.string.onramp_all_offers_button_title),
            subtitle = TextReference.Res(R.string.express_choose_providers_title),
            startIconRes = R.drawable.ic_back_24,
            onStartClick = onBackClick,
            endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
            onEndClick = onCloseClick,
        )
    }
}

@Composable
private fun PaymentMethodTitle(onCloseClick: () -> Unit) {
    if (LocalOnrampDesignSystemEnabled.current) {
        TangemModal.TopNavigation(
            title = TextReference.Res(R.string.onramp_all_offers_button_title),
            subtitle = TextReference.Res(R.string.onramp_payment_method_subtitle),
            onClose = onCloseClick,
        )
    } else {
        TangemModalBottomSheetTitle(
            title = TextReference.Res(R.string.onramp_all_offers_button_title),
            subtitle = TextReference.Res(R.string.onramp_payment_method_subtitle),
            endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
            onEndClick = onCloseClick,
        )
    }
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
            OnrampOffersNotification(notification = notification)
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
            .then(
                if (LocalOnrampDesignSystemEnabled.current) {
                    Modifier
                } else {
                    Modifier.background(TangemTheme.colors3.bg.primary)
                },
            )
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(times = LOADING_ITEMS_COUNT) {
            if (LocalOnrampDesignSystemEnabled.current) {
                ProvideTangemShimmer {
                    TangemShimmer(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .height(96.dp),
                        radius = 14.dp,
                    )
                }
            } else {
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
    }
}

@Composable
private fun OnrampOffersNotification(notification: NotificationUM) {
    if (LocalOnrampDesignSystemEnabled.current) {
        TangemMessage(config = notification.config)
    } else {
        Notification(config = notification.config)
    }
}

private const val LOADING_ITEMS_COUNT = 3

@Composable
fun AllOffersError(errorNotification: NotificationUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        OnrampOffersNotification(notification = errorNotification)
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