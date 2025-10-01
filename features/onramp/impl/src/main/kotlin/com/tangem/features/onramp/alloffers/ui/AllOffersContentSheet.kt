package com.tangem.features.onramp.alloffers.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.onramp.alloffers.entity.AllOffersPaymentMethodUM
import com.tangem.features.onramp.alloffers.entity.AllOffersStateUM
import com.tangem.features.onramp.alloffers.entity.OnrampPaymentMethodConfig
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferUM
import com.tangem.features.onramp.mainv2.ui.Offer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun AllOffersContentSheet(state: AllOffersStateUM, onCloseClick: () -> Unit) {
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
        containerColor = TangemTheme.colors.background.tertiary,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .animateContentSize(),
            ) {
                AnimatedContent(
                    targetState = state is AllOffersStateUM.Content && state.currentMethod != null,
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 220)) togetherWith
                            fadeOut(tween(durationMillis = 220))
                    },
                    label = "Change offers and payment method state",
                ) { shouldShowOffersScreen ->
                    when (state) {
                        AllOffersStateUM.Loading -> AllOffersContentLoading()
                        is AllOffersStateUM.Error -> AllOffersError(state.errorNotification)
                        is AllOffersStateUM.Content -> {
                            if (shouldShowOffersScreen) {
                                state.currentMethod?.let {
                                    OffersBasedOnPaymentMethodContent(offers = it.offers)
                                }
                            } else {
                                PaymentMethodsContent(methods = state.methods)
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ProviderTitle(onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    TangemModalBottomSheetTitle(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.express_choose_providers_title),
        startIconRes = R.drawable.ic_back_24,
        onStartClick = onBackClick,
        endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
        onEndClick = onCloseClick,
    )
}

@Composable
private fun PaymentMethodTitle(onCloseClick: () -> Unit) {
    TangemModalBottomSheetTitle(
        title = TextReference.Res(R.string.onramp_all_offers_button_title),
        subtitle = TextReference.Res(R.string.onramp_payment_method_subtitle),
        endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
        onEndClick = onCloseClick,
    )
}

@Composable
private fun OffersBasedOnPaymentMethodContent(offers: ImmutableList<OnrampOfferUM>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        offers.fastForEach { offer ->
            key("${offer.paymentMethod.id} ${offer.providerName} ${offer.rate}") {
                Offer(offer)
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
            .background(TangemTheme.colors.background.tertiary)
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

@Composable
fun AllOffersError(errorNotification: NotificationUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Notification(config = errorNotification.config)
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
                providerId = "providerId1",
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
                providerId = "providerId2",
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
    )

    TangemThemePreview {
        AllOffersContentSheet(
            state = AllOffersStateUM.Content(
                methods = persistentListOf(),
                currentMethod = method,
                onBackClicked = {},
            ),
            onCloseClick = {},
        )
    }
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
                    providerId = "providerId1",
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
                    providerId = "providerId2",
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
        )
    }

    TangemThemePreview {
        AllOffersContentSheet(
            state = AllOffersStateUM.Content(
                methods = methods.toPersistentList(),
                currentMethod = null,
                onBackClicked = {},
            ),
            onCloseClick = {},
        )
    }
}