package com.tangem.features.onramp.main.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.test.OnrampOffersBlockTestTags
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.*
import com.tangem.features.onramp.paymentmethod.ui.themedImageUrl
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.persistentListOf

@Composable
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
internal fun OnrampAllOffersButtonDs(config: OnrampAllOffersButtonConfig) {
    TangemButton(
        modifier = Modifier.fillMaxWidth(),
        variant = TangemButton.Variant.Secondary,
        text = config.title,
        onClick = config.onClick,
    )
}

@Composable
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
internal fun OfferDs(onrampOfferUM: OnrampOfferUM, modifier: Modifier = Modifier, roundBottom: Boolean = true) {
    val shape = if (roundBottom) {
        RoundedCornerShape(14.dp)
    } else {
        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    }
    val isBuyEnabled = onrampOfferUM.advantages !is OnrampOfferAdvantagesUM.Unavailable &&
        onrampOfferUM.advantages != OnrampOfferAdvantagesUM.Restricted
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = shape,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    OfferHeader(advantage = onrampOfferUM.advantages)
                    RateBlock(
                        rate = onrampOfferUM.rate,
                        diff = onrampOfferUM.diff
                            .takeIf { onrampOfferUM.advantages != OnrampOfferAdvantagesUM.Restricted },
                        isOfferUnavailable = onrampOfferUM.advantages is OnrampOfferAdvantagesUM.Unavailable,
                    )
                }
                SpacerWMax()
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X9,
                    text = resourceReference(R.string.common_buy),
                    onClick = onrampOfferUM.onBuyClicked,
                    isEnabled = isBuyEnabled,
                )
            }
            SpacerH(10.dp)
            HorizontalDivider(color = TangemTheme.colors3.border.secondary)
            SpacerH(12.dp)
            PaymentBlockInOfferDs(
                paymentMethod = onrampOfferUM.paymentMethod,
                providerName = onrampOfferUM.providerName,
            )
        }
    }
}

@Composable
private fun PaymentBlockInOfferDs(paymentMethod: OnrampPaymentMethod, providerName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier
                .size(16.dp)
                .testTag(OnrampOffersBlockTestTags.TIMING_ICON),
            imageVector = ImageVector.vectorResource(R.drawable.ic_clock_24),
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )

        SpacerW(2.dp)

        TimingBlock(speed = paymentMethod.type.getProcessingSpeed())

        SpacerW(6.dp)

        DrawDot(color = TangemTheme.colors3.text.secondary)

        SpacerW(6.dp)

        Text(
            text = stringResourceSafe(R.string.onramp_via_provider, providerName),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.testTag(OnrampOffersBlockTestTags.PROVIDER_NAME),
        )

        SpacerWMax()

        Text(
            text = stringResourceSafe(R.string.onramp_pay_with),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.testTag(OnrampOffersBlockTestTags.PAY_WITH),
        )

        SpacerW(4.dp)

        SubcomposeAsyncImage(
            modifier = Modifier
                .size(width = 38.dp, height = 16.dp)
                .testTag(OnrampOffersBlockTestTags.PAYMENT_METHOD_ICON),
            alignment = Alignment.CenterEnd,
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(paymentMethod.themedImageUrl())
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