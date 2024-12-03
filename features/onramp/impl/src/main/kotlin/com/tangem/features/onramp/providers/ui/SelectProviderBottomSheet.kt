package com.tangem.features.onramp.providers.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.paymentmethod.ui.PaymentMethodIcon
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import com.tangem.features.onramp.providers.entity.ProviderListPaymentMethodUM
import com.tangem.features.onramp.providers.entity.ProviderListUM

@Composable
internal fun SelectProviderBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        titleText = resourceReference(R.string.express_choose_providers_title),
        content = { content() },
    )
}

@Composable
internal fun SelectProviderBottomSheetContent(state: ProviderListUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.onramp_choose_provider_title_hint),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        PaymentMethodBlock(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing20, bottom = TangemTheme.dimens.spacing16),
            state = state.paymentMethod,
        )
        state.providers.fastForEach { provider ->
            key(provider.providerId) { ProviderItem(state = provider) }
        }
    }
}

@Composable
private fun PaymentMethodBlock(state: ProviderListPaymentMethodUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
            .border(
                width = TangemTheme.dimens.size1,
                color = TangemTheme.colors.stroke.secondary,
                shape = RoundedCornerShape(TangemTheme.dimens.radius16),
            )
            .clickable(onClick = state.onClick)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        PaymentMethodIcon(imageUrl = state.imageUrl)
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(id = R.string.onramp_pay_with),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
            Text(
                text = state.name,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.primary1,
            )
        }
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size24),
            painter = painterResource(id = R.drawable.ic_chevron_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun ProviderItem(state: ProviderListItemUM, modifier: Modifier = Modifier) {
    when (state) {
        is ProviderListItemUM.Available -> AvailableProviderItem(
            modifier = modifier
                .clickable(onClick = state.onClick)
                .padding(all = TangemTheme.dimens.spacing12),
            state = state,
        )
        is ProviderListItemUM.Unavailable -> UnavailableProviderItem(
            modifier = modifier.padding(all = TangemTheme.dimens.spacing12),
            state = state,
        )
    }
}

@Composable
private fun AvailableProviderItem(state: ProviderListItemUM.Available, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size40)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(state.imageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1F),
            text = state.name,
            style = TangemTheme.typography.subtitle2,
            color = if (state.isSelected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
        Text(
            text = state.rate,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

private const val UNAVAILABLE_ALPHA = 0.4F

@Composable
private fun UnavailableProviderItem(state: ProviderListItemUM.Unavailable, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size40)
                .clip(TangemTheme.shapes.roundedCorners8)
                .alpha(UNAVAILABLE_ALPHA),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(state.imageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = state.name,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.secondary,
            )
            Text(
                text = state.subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}