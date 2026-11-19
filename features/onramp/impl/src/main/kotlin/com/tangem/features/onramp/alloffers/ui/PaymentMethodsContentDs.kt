package com.tangem.features.onramp.alloffers.ui

import android.content.res.Configuration
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_24
import com.tangem.core.ui.res.generated.icons.ic_stack_24
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
import com.tangem.features.onramp.paymentmethod.ui.themedImageUrl
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
            RegionRestrictionBannerDs(notification = notification)
            SpacerH(4.dp)
        }
        methods.fastForEach { method ->
            key(method.methodConfig.method.id) {
                PaymentMethodDs(methodUM = method)
                SpacerH(4.dp)
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
        color = TangemTheme.colors3.bg.transparent.primary,
        shape = RoundedCornerShape(24.dp),
        onClick = methodUM.methodConfig.onClick,
    ) {
        PaymentMethodBodyDs(
            methodUM = methodUM,
            modifier = Modifier.fillMaxWidth().padding(PaymentMethodContentPaddingDs),
        )
    }
}

private val PaymentMethodContentPaddingDs = PaddingValues(all = 16.dp)

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun PaymentMethodBodyDs(methodUM: AllOffersPaymentMethodUM, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        PaymentMethodIconDs(method = methodUM.methodConfig.method)

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
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProvidersCountBlockInfoDs(providersCount = methodUM.providersCount)
                        SpacerW(4.dp)
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
private fun PaymentMethodIconDs(method: OnrampPaymentMethod, modifier: Modifier = Modifier) {
    val tileColor = if (method.hasThemedImages) {
        TangemTheme.colors3.bg.transparent.primary
    } else {
        TangemColorPalette.Light1
    }
    SubcomposeAsyncImage(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tileColor)
            .padding(10.dp)
            .testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_ICON),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(method.themedImageUrl())
            .crossfade(enable = true)
            .allowHardware(false)
            .build(),
        contentDescription = null,
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun UnavailablePaymentMethodInfoBlockDs(
    paymentMethodName: String,
    errorAmount: String,
    unavailableStatus: PaymentMethodStatus.Unavailable,
) {
    Column {
        Text(
            text = paymentMethodName,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_NAME),
        )
        SpacerH(4.dp)
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
    Column {
        Text(
            text = paymentMethodName,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.testTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_NAME),
        )
        SpacerH(4.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.onramp_up_to_rate),
                style = TangemTheme.typography3.subheading.medium,
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
                                color = TangemTheme.colors3.bg.status.errorSubtle,
                                shape = RoundedCornerShape(percent = 50),
                            )
                            .padding(horizontal = 2.dp),
                        text = diff.resolveReference(),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.status.error,
                    )
                }
            }
        }
    }
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun ProvidersCountBlockInfoDs(providersCount: Int) {
    ChipRowDs {
        Icon(
            modifier = Modifier
                .size(12.dp)
                .testTag(SelectPaymentMethodBottomSheetTestTags.PROVIDER_COUNT_ICON),
            imageVector = Icons.ic_stack_24,
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
private fun ChipRowDs(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 16.dp)
            .background(
                color = TangemTheme.colors3.bg.transparent.primary,
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
private fun TimingBlockInfoDs(speed: PaymentMethodType.PaymentSpeed) {
    ChipRowDs {
        Icon(
            modifier = Modifier
                .size(12.dp)
                .testTag(SelectPaymentMethodBottomSheetTestTags.TIMING_ICON),
            imageVector = Icons.ic_clock_24,
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
        SpacerW(4.dp)
        TimingBlock(speed)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
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