package com.tangem.features.onramp.alloffers.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SelectPaymentMethodBottomSheetTestTags
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodStatus
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.onramp.alloffers.entity.AllOffersPaymentMethodUM
import com.tangem.features.onramp.alloffers.entity.OnrampPaymentMethodConfig
import com.tangem.features.onramp.alloffers.entity.OnrampRegionRestrictionNotification
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.main.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.main.entity.OnrampOfferUM
import com.tangem.features.onramp.main.ui.TimingBlock
import com.tangem.features.onramp.paymentmethod.ui.PaymentMethodIcon
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
internal fun PaymentMethodsContentDs(methods: ImmutableList<AllOffersPaymentMethodUM>, notification: NotificationUM?) {
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
        methods.fastForEach { method ->
            key(method.methodConfig.method.id) {
                PaymentMethodDs(methodUM = method)
                SpacerH(8.dp)
            }
        }
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun PaymentMethodDs(methodUM: AllOffersPaymentMethodUM, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD),
        color = TangemTheme.colors3.bg.secondary,
        shape = RoundedCornerShape(14.dp),
        onClick = methodUM.methodConfig.onClick,
    ) {
        PaymentMethodBodyDs(
            methodUM = methodUM,
            modifier = Modifier.fillMaxWidth().padding(PaymentMethodContentPaddingDs),
        )
    }
}

private val PaymentMethodContentPaddingDs = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun PaymentMethodBodyDs(methodUM: AllOffersPaymentMethodUM, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        PaymentMethodIcon(
            modifier = Modifier
                .size(36.dp)
                .testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_ICON),
            method = methodUM.methodConfig.method,
        )

        SpacerW(12.dp)

        Column {
            when (methodUM.paymentMethodStatus) {
                PaymentMethodStatus.Available -> {
                    PaymentMethodInfoBlockDs(
                        paymentMethodName = methodUM.methodConfig.method.name,
                        rate = methodUM.rate,
                        diff = methodUM.diff,
                        isBestRate = methodUM.isBestRate,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProvidersCountBlockInfoDs(providersCount = methodUM.providersCount)
                        SpacerW(8.dp)
                        TimingBlockInfoDs(speed = methodUM.methodConfig.method.type.getProcessingSpeed())
                    }
                }
                is PaymentMethodStatus.Unavailable -> {
                    UnavailablePaymentMethodInfoBlockDs(
                        paymentMethodName = methodUM.methodConfig.method.name,
                        errorAmount = methodUM.rate,
                        unavailableStatus = methodUM.paymentMethodStatus,
                    )
                }
            }
        }
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun UnavailablePaymentMethodInfoBlockDs(
    paymentMethodName: String,
    errorAmount: String,
    unavailableStatus: PaymentMethodStatus.Unavailable,
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = paymentMethodName,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_NAME),
        )
        SpacerH(2.dp)
        val messageResId = when (unavailableStatus) {
            is PaymentMethodStatus.Unavailable.MinAmount -> R.string.onramp_provider_min_amount
            is PaymentMethodStatus.Unavailable.MaxAmount -> R.string.onramp_provider_max_amount
        }
        Text(
            text = stringResourceSafe(messageResId, errorAmount),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.UP_TO_TEXT),
        )
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun PaymentMethodInfoBlockDs(
    paymentMethodName: String,
    rate: String,
    diff: TextReference?,
    isBestRate: Boolean,
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = paymentMethodName,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_NAME),
        )
        SpacerH(2.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.onramp_up_to_rate),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors3.text.secondary,
                modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.UP_TO_TEXT),
            )

            Text(
                text = rate,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.TOKEN_AMOUNT),
            )

            when {
                isBestRate -> {
                    Image(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_best_rate_12),
                        contentDescription = null,
                        modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.BEST_RATE_ICON),
                    )
                }
                diff != null -> {
                    Text(
                        modifier = Modifier
                            .background(
                                color = TangemTheme.colors3.text.accent.red.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 4.dp),
                        text = diff.resolveReference(),
                        style = TangemTheme.typography.caption1,
                        color = TangemTheme.colors3.text.accent.red,
                    )
                }
            }
        }
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun ProvidersCountBlockInfoDs(providersCount: Int) {
    BorderedRow {
        Icon(
            modifier = Modifier
                .size(10.dp)
                .testTag(SelectPaymentMethodBottomSheetTestTags.PROVIDER_COUNT_ICON),
            imageVector = ImageVector.vectorResource(R.drawable.ic_clock_24),
            tint = TangemTheme.colors3.icon.secondary,
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
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.PROVIDER_COUNT_TEXT),
        )
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun TimingBlockInfoDs(speed: PaymentMethodType.PaymentSpeed) {
    BorderedRow {
        Icon(
            modifier = Modifier
                .size(10.dp)
                .testTag(SelectPaymentMethodBottomSheetTestTags.TIMING_ICON),
            imageVector = ImageVector.vectorResource(R.drawable.ic_staking_24),
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
        SpacerW(4.dp)
        TimingBlock(speed)
    }
}

@Preview
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun PaymentMethodsContentPreviewDs() {
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
        PaymentMethodsContentDs(
            methods = persistentListOf(method),
            notification = OnrampRegionRestrictionNotification,
        )
    }
}