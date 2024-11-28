package com.tangem.features.onramp.main.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.extensions.appendSpace
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampProviderBlockUM

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
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing12)
                .size(size = TangemTheme.dimens.size40)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(state.paymentMethod.imageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(id = R.string.onramp_pay_with))
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
            )
            Text(
                text = buildAnnotatedString {
                    append(stringResource(id = R.string.onramp_via))
                    appendSpace()
                    append(state.providerName)
                },
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
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
            text = stringResource(id = R.string.express_provider_best_rate),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.primary2,
        )
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
            text = stringResource(id = R.string.express_provider),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
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
                text = stringResource(id = R.string.express_fetch_best_rates),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}
