package com.tangem.features.onramp.alloffers.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.onramp.alloffers.entity.AllOffersPaymentMethodUM
import com.tangem.features.onramp.alloffers.entity.OnrampPaymentMethodConfig
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.mainv2.entity.OnrampOfferUM
import com.tangem.features.onramp.mainv2.ui.TimingBlock
import com.tangem.features.onramp.paymentmethod.ui.PaymentMethodIcon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PaymentMethodsContent(methods: ImmutableList<AllOffersPaymentMethodUM>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        methods.fastForEach { method ->
            key(method.methodConfig.method.id) {
                PaymentMethod(methodUM = method)
                SpacerH(8.dp)
            }
        }
    }
}

@Composable
private fun PaymentMethod(methodUM: AllOffersPaymentMethodUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.action,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = methodUM.methodConfig.onClick,
            )
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 14.dp,
                bottom = 12.dp,
            ),
    ) {
        PaymentMethodIcon(
            modifier = Modifier.size(36.dp),
            imageUrl = methodUM.methodConfig.method.imageUrl,
        )

        SpacerW(12.dp)

        Column {
            PaymentMethodInfoBlock(
                paymentMethodName = methodUM.methodConfig.method.name,
                rate = methodUM.rate,
                diff = methodUM.diff,
                isBestRate = methodUM.isBestRate,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProvidersCountBlockInfo(providersCount = methodUM.providersCount)
                SpacerW(8.dp)
                TimingBlockInfo(speed = methodUM.methodConfig.method.type.getProcessingSpeed())
            }
        }
    }
}

@Composable
private fun PaymentMethodInfoBlock(
    paymentMethodName: String,
    rate: String,
    diff: TextReference?,
    isBestRate: Boolean,
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = paymentMethodName,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH(2.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.onramp_up_to_rate),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )

            Text(
                text = rate,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )

            when {
                isBestRate -> {
                    Image(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_best_rate_12),
                        contentDescription = null,
                    )
                }
                diff != null -> {
                    Text(
                        modifier = Modifier
                            .background(
                                color = TangemTheme.colors.text.warning.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 4.dp),
                        text = diff.resolveReference(),
                        style = TangemTheme.typography.caption1,
                        color = TangemTheme.colors.text.warning,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvidersCountBlockInfo(providersCount: Int) {
    BorderedRow {
        Icon(
            modifier = Modifier.size(10.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_clock_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
        SpacerW(4.dp)
        Text(
            text = pluralStringResourceSafe(
                id = R.plurals.onramp_providers_count,
                count = providersCount,
                providersCount,
            ),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun TimingBlockInfo(speed: PaymentMethodType.PaymentSpeed) {
    BorderedRow {
        Icon(
            modifier = Modifier.size(10.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_staking_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
        SpacerW(4.dp)
        TimingBlock(speed)
    }
}

@Composable
fun BorderedRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = TangemTheme.colors.stroke.primary,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Preview
@Composable
private fun PaymentMethodsContentPreview() {
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
        PaymentMethodsContent(persistentListOf(method))
    }
}