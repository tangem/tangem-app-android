package com.tangem.features.onramp.main.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.extensions.appendSpace
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.BuyTokenDetailsScreenTestTags
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampProviderBlockUM
import com.tangem.features.onramp.paymentmethod.ui.PaymentMethodIcon

@Composable
internal fun OnrampProviderContent(state: OnrampProviderBlockUM, modifier: Modifier = Modifier) {
    when (state) {
        is OnrampProviderBlockUM.Empty -> Unit
        is OnrampProviderBlockUM.Loading -> OnrampProviderLoading(modifier)
        is OnrampProviderBlockUM.Content -> OnrampProviderBlock(modifier = modifier, state = state)
    }
}

@Composable
private fun OnrampProviderBlock(state: OnrampProviderBlockUM.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = TangemTheme.dimens.radius16))
            .background(TangemTheme.colors.background.action)
            .clickable(onClick = state.onClick)
            .padding(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        PaymentMethodIcon(imageUrl = state.paymentMethod.imageUrl)
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = buildAnnotatedString {
                    append(stringResourceSafe(id = R.string.onramp_pay_with))
                    appendSpace()
                    withStyle(
                        style = SpanStyle(
                            fontWeight = TangemTheme.typography.subtitle2.fontWeight,
                            color = TangemTheme.colors.text.primary1,
                        ),
                    ) {
                        append(state.paymentMethod.name)
                    }
                },
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                modifier = Modifier.testTag(BuyTokenDetailsScreenTestTags.PROVIDER_TITLE),
            )
            Text(
                text = buildAnnotatedString {
                    append(stringResourceSafe(id = R.string.onramp_via))
                    appendSpace()
                    append(state.providerName)
                },
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.testTag(BuyTokenDetailsScreenTestTags.PROVIDER_TEXT),
            )
        }
        AnimatedVisibility(
            visible = state.isBestRate,
            enter = fadeIn(),
            exit = fadeOut(),
            label = "Best Rate visibility animation",
        ) {
            Text(
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.icon.accent,
                        shape = RoundedCornerShape(TangemTheme.dimens.radius4),
                    )
                    .padding(
                        horizontal = TangemTheme.dimens.spacing6,
                        vertical = TangemTheme.dimens.spacing1,
                    ),
                text = stringResourceSafe(id = R.string.express_provider_best_rate),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.primary2,
            )
        }
    }
}

@Composable
private fun OnrampProviderLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = TangemTheme.dimens.radius16))
            .background(TangemTheme.colors.background.action)
            .padding(TangemTheme.dimens.spacing12),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.express_provider),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.testTag(BuyTokenDetailsScreenTestTags.PROVIDER_LOADING_TITLE),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            CircularProgressIndicator(
                color = TangemTheme.colors.icon.informative,
                strokeWidth = TangemTheme.dimens.size2,
                modifier = Modifier.size(TangemTheme.dimens.size16),
            )
            Text(
                text = stringResourceSafe(id = R.string.express_fetch_best_rates),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.testTag(BuyTokenDetailsScreenTestTags.PROVIDER_LOADING_TEXT),
            )
        }
    }
}