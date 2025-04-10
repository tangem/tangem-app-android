package com.tangem.features.onramp.providers.ui

import android.content.res.Configuration
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.paymentmethod.ui.PaymentMethodIcon
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import com.tangem.features.onramp.providers.entity.SelectPaymentAndProviderUM
import com.tangem.features.onramp.providers.model.previewData.SelectProviderPreviewData
import com.tangem.features.onramp.utils.selectedBorder

@Composable
internal fun SelectProviderBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        titleText = resourceReference(R.string.express_choose_providers_title),
        content = { content() },
    )
}

@Composable
internal fun SelectProviderBottomSheetContent(state: SelectPaymentAndProviderUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(id = R.string.onramp_choose_provider_title_hint),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        PaymentMethodBlock(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing20, bottom = TangemTheme.dimens.spacing16),
            state = state.selectedPaymentMethod.paymentMethod,
            isClickEnabled = state.isPaymentMethodClickEnabled,
            onPaymentMethodClick = state.onPaymentMethodClick,
        )
        state.selectedPaymentMethod.providers.fastForEach { provider ->
            key(provider.providerId) { ProviderItem(state = provider) }
        }
        OnrampMoreProviders()
    }
}

@Composable
private fun PaymentMethodBlock(
    state: OnrampPaymentMethod,
    isClickEnabled: Boolean,
    onPaymentMethodClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
            .border(
                width = TangemTheme.dimens.size1,
                color = TangemTheme.colors.stroke.secondary,
                shape = RoundedCornerShape(TangemTheme.dimens.radius16),
            )
            .clickable(enabled = isClickEnabled, onClick = onPaymentMethodClick)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        PaymentMethodIcon(imageUrl = state.imageUrl)
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResourceSafe(id = R.string.onramp_pay_with),
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
        is ProviderListItemUM.Available.Content -> AvailableProviderItem(
            modifier = modifier,
            state = state,
        )
        is ProviderListItemUM.Available.WithError -> {
            UnavailableProviderItem(
                modifier = modifier.clickable(onClick = state.onClick),
                imageUrl = state.imageUrl,
                providerName = state.name,
                subtitle = state.subtitle,
                isSelected = state.isSelected,
            )
        }
        is ProviderListItemUM.Unavailable -> UnavailableProviderItem(
            modifier = modifier,
            imageUrl = state.imageUrl,
            providerName = state.name,
            subtitle = state.subtitle,
            isSelected = false,
        )
    }
}

@Composable
private fun AvailableProviderItem(state: ProviderListItemUM.Available.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .then(
                if (state.isSelected) {
                    Modifier.selectedBorder()
                } else {
                    Modifier.clip(RoundedCornerShape(16.dp))
                },
            )
            .clickable(onClick = state.onClick)
            .padding(all = TangemTheme.dimens.spacing12),
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
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.rate,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            when {
                state.isBestRate -> {
                    Text(
                        text = stringResourceSafe(R.string.express_provider_best_rate),
                        style = TangemTheme.typography.caption1,
                        color = TangemTheme.colors.text.constantWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TangemTheme.colors.icon.accent)
                            .padding(vertical = 1.dp, horizontal = 6.dp),
                    )
                }
                state.diffRate != null -> {
                    Text(
                        text = state.diffRate.resolveReference(),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.warning,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

private const val UNAVAILABLE_ALPHA = 0.4F

@Composable
private fun UnavailableProviderItem(
    imageUrl: String,
    providerName: String,
    subtitle: TextReference,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .then(
                if (isSelected) {
                    Modifier.selectedBorder()
                } else {
                    Modifier.clip(RoundedCornerShape(16.dp))
                },
            )
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size40)
                .clip(TangemTheme.shapes.roundedCorners8)
                .alpha(UNAVAILABLE_ALPHA),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(imageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = providerName,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.secondary,
            )
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun OnrampMoreProviders() {
    Icon(
        painter = rememberVectorPainter(
            ImageVector.vectorResource(R.drawable.ic_lightning_16),
        ),
        contentDescription = null,
        tint = TangemTheme.colors.icon.informative,
        modifier = Modifier
            .padding(top = 16.dp),
    )
    Text(
        text = stringResourceSafe(R.string.express_more_providers_soon),
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.icon.informative,
        modifier = Modifier
            .padding(top = 4.dp, bottom = 24.dp)
            .padding(horizontal = TangemTheme.dimens.spacing56),
        textAlign = TextAlign.Center,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectProviderBottomSheetContent_Preview(
    @PreviewParameter(SelectProviderBottomSheetContentPreviewProvider::class)
    data: SelectPaymentAndProviderUM,
) {
    TangemThemePreview {
        SelectProviderBottomSheetContent(data)
    }
}

private class SelectProviderBottomSheetContentPreviewProvider :
    PreviewParameterProvider<SelectPaymentAndProviderUM> {
    override val values: Sequence<SelectPaymentAndProviderUM>
        get() = sequenceOf(
            SelectProviderPreviewData.state,
        )
}
// endregion