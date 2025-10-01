package com.tangem.features.onramp.mainv2.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferUM
import com.tangem.features.onramp.mainv2.entity.OnrampOffersBlockUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun OnrampOffersContent(state: OnrampOffersBlockUM) {
    AnimatedVisibility(
        visible = state.isBlockVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300),
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300),
        ),
        label = "Offers block animation",
    ) {
        if (state is OnrampOffersBlockUM.Content) {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.recentOffer?.let { recentOffer ->
                    Column {
                        Text(
                            modifier = Modifier.padding(start = 12.dp),
                            text = stringResourceSafe(R.string.onramp_recently_used_title),
                            style = TangemTheme.typography.subtitle2,
                            color = TangemTheme.colors.text.tertiary,
                        )

                        SpacerH(8.dp)

                        Offer(recentOffer)

                        SpacerH(16.dp)
                    }
                }

                Text(
                    modifier = Modifier.padding(start = 12.dp),
                    text = stringResourceSafe(R.string.onramp_recommended_title),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                )

                SpacerH(8.dp)

                state.recommended.fastForEach { offer ->
                    key("${offer.paymentMethod.id} ${offer.providerName} ${offer.rate}") {
                        Offer(offer)
                        SpacerH(8.dp)
                    }
                }

                state.onrampAllOffersButtonConfig?.let {
                    SpacerH(12.dp)
                    SecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = it.title.resolveReference(),
                        onClick = it.onClick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun Offer(onrampOfferUM: OnrampOfferUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.action,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                OfferHeader(advantage = onrampOfferUM.advantages)
                RateBlock(rate = onrampOfferUM.rate, diff = onrampOfferUM.diff)
            }
            SpacerWMax()
            SecondaryButton(
                size = TangemButtonSize.RoundedAction,
                text = stringResourceSafe(R.string.common_buy),
                onClick = onrampOfferUM.onBuyClicked,
            )
        }
        SpacerH(10.dp)
        HorizontalDivider(color = TangemTheme.colors.stroke.primary)
        SpacerH(12.dp)
        PaymentBlockInOffer(
            paymentMethod = onrampOfferUM.paymentMethod,
            providerName = onrampOfferUM.providerName,
        )
    }
}

@Composable
private fun OfferHeader(advantage: OnrampOfferAdvantagesUM) {
    when (advantage) {
        OnrampOfferAdvantagesUM.Default -> {
            Text(
                text = stringResourceSafe(R.string.onramp_title_you_get),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        OnrampOfferAdvantagesUM.BestRate -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_best_rate_16),
                    tint = TangemTheme.colors.icon.accent,
                    contentDescription = null,
                )
                Text(
                    text = stringResourceSafe(R.string.express_provider_best_rate),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.icon.accent,
                )
            }
        }
        OnrampOfferAdvantagesUM.Fastest -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_fastest_16),
                    tint = TangemTheme.colors.icon.attention,
                    contentDescription = null,
                )
                Text(
                    text = stringResourceSafe(R.string.onramp_offer_type_fastet),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.icon.attention,
                )
            }
        }
    }
}

@Composable
private fun RateBlock(rate: String, diff: TextReference?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = rate,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        diff?.let {
            Text(
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.text.warning.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 4.dp),
                text = it.resolveReference(),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.warning,
            )
        }
    }
}

@Composable
private fun PaymentBlockInOffer(paymentMethod: OnrampPaymentMethod, providerName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_clock_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )

        SpacerW(2.dp)

        TimingBlock(speed = paymentMethod.type.getProcessingSpeed())

        SpacerW(6.dp)

        DrawDot(color = TangemTheme.colors.text.tertiary)

        SpacerW(6.dp)

        Text(
            text = providerName,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )

        SpacerWMax()

        Text(
            text = stringResourceSafe(R.string.onramp_pay_with),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )

        SpacerW(4.dp)

        SubcomposeAsyncImage(
            modifier = Modifier.sizeIn(maxWidth = 38.dp, maxHeight = 16.dp),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(paymentMethod.imageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = {
                TextShimmer(
                    style = TangemTheme.typography.body1,
                    modifier = Modifier.width(40.dp),
                )
            },
            contentDescription = null,
        )
    }
}

@Composable
internal fun TimingBlock(speed: PaymentMethodType.PaymentSpeed) {
    val timingText = when (speed) {
        PaymentMethodType.PaymentSpeed.Instant -> {
            stringResourceSafe(id = R.string.onramp_instant_status)
        }
        PaymentMethodType.PaymentSpeed.FewMin -> {
            stringResourceSafe(
                id = R.string.onramp_timing_minutes,
                FEW_MINS_VALUE,
            )
        }
        PaymentMethodType.PaymentSpeed.FewDays -> {
            pluralStringResourceSafe(
                id = R.plurals.onramp_timing_days,
                count = FEW_DAYS_VALUE,
                FEW_DAYS_VALUE,
            )
        }
        PaymentMethodType.PaymentSpeed.PlentyDays -> {
            pluralStringResourceSafe(
                id = R.plurals.onramp_timing_days,
                count = PLENTY_DAYS_VALUE,
                PLENTY_DAYS_VALUE,
            )
        }
        PaymentMethodType.PaymentSpeed.Unknown -> ""
    }

    Text(
        text = timingText,
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
fun DrawDot(color: Color) {
    Spacer(
        modifier = Modifier
            .size(4.dp)
            .drawWithCache {
                val radius = size.minDimension / 2f
                onDrawBehind {
                    drawCircle(
                        color = color,
                        radius = radius,
                    )
                }
            },
    )
}

// will be hardcoded till server will be ready to provide this values
private const val FEW_MINS_VALUE = "3-5"
private const val FEW_DAYS_VALUE = 3
private const val PLENTY_DAYS_VALUE = 5

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnrampOffersContentPreview() {
    val state = OnrampOffersBlockUM.Content(
        isBlockVisible = true,
        recentOffer = OnrampOfferUM(
            category = OnrampOfferCategoryUM.RecentlyUsed,
            advantages = OnrampOfferAdvantagesUM.Default,
            paymentMethod = OnrampPaymentMethod(
                id = "card",
                name = "Card",
                imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
                type = PaymentMethodType.CARD,
            ),
            providerId = "providerId3",
            providerName = "Simplex",
            rate = "0,00045334 BTC",
            diff = stringReference("–27%"),
            onBuyClicked = {},
        ),
        recommended = persistentListOf(
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
                advantages = OnrampOfferAdvantagesUM.Fastest,
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
        onrampAllOffersButtonConfig = null,
    )
    TangemThemePreview {
        OnrampOffersContent(state)
    }
}