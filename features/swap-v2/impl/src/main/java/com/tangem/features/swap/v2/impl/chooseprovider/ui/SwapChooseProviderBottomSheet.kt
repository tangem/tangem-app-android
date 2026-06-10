package com.tangem.features.swap.v2.impl.chooseprovider.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.provider.ProviderTypeFilterPicker
import com.tangem.core.ui.components.provider.entity.ProviderChooseUM
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.selectedBorder
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapChooseProviderBottomSheetContent
import com.tangem.features.swap.v2.impl.chooseprovider.ui.preview.SwapChooseProviderContentPreview
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.notifications.entity.SwapNotificationUM
import kotlinx.collections.immutable.persistentListOf

private const val DISABLED_COLORS_ALPHA = 0.5f

@Composable
internal fun SwapChooseProviderBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent>(
        config = config,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.express_choose_provider),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = config.onDismissRequest,
            )
        },
    ) {
        content()
    }
}

@Suppress("LongMethod")
@Composable
internal fun SwapChooseProviderContent(
    contentUM: SwapChooseProviderBottomSheetContent,
    onProviderClick: (SwapQuoteUM) -> Unit,
    onFilterSelect: (ProviderFilterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var minHeight by remember { mutableStateOf(0.dp) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 12.dp)
            .heightIn(min = minHeight)
            .onSizeChanged { size ->
                with(density) {
                    val h = size.height.toDp()
                    if (h > minHeight) minHeight = h
                }
            },
    ) {
        if (contentUM.availableFilters.isNotEmpty()) {
            ProviderTypeFilterPicker(
                availableFilters = contentUM.availableFilters,
                selectedFilter = contentUM.selectedFilter,
                onFilterSelect = onFilterSelect,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 12.dp),
            )
        }
        AnimatedVisibility(
            modifier = Modifier.padding(top = 12.dp),
            visible = contentUM.isApplyFCARestrictions,
        ) {
            Notification(
                config = SwapNotificationUM.Error.FCAWarningList.config,
                containerColor = TangemTheme.colors.button.disabled,
                iconTint = TangemTheme.colors.icon.warning,
            )
        }
        SpacerH12()
        contentUM.providerList.fastForEach { provider ->
            SwapProviderItem(
                state = provider.swapProviderState,
                modifier = Modifier
                    .selectedBorder(isSelected = provider.swapProviderState.isSelected)
                    .clickable(
                        enabled = provider.quote !is SwapQuoteUM.Error,
                        onClick = { onProviderClick(provider.quote) },
                    )
                    .padding(12.dp)
                    .conditional(provider.providerUM.extraUM is ProviderChooseUM.ExtraUM.Error) {
                        Modifier.alpha(DISABLED_COLORS_ALPHA)
                    },
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_lightning_16),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier.padding(
                top = 16.dp,
                start = 3.dp,
                end = 3.dp,
            ),
        )
        Text(
            text = stringResourceSafe(R.string.express_more_providers_soon),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.icon.informative,
            modifier = Modifier
                .padding(
                    top = 4.dp,
                    bottom = 24.dp,
                    start = 3.dp,
                    end = 3.dp,
                ),
            textAlign = TextAlign.Center,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapChooseProviderContent_Preview(
    @PreviewParameter(ProviderChooseCryptoPreviewProvider::class) params: SwapChooseProviderBottomSheetContent,
) {
    TangemThemePreview {
        SwapChooseProviderBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
        ) {
            SwapChooseProviderContent(
                contentUM = SwapChooseProviderBottomSheetContent(
                    providerList = params.providerList,
                    isApplyFCARestrictions = true,
                    selectedProvider = SwapChooseProviderContentPreview.provider1,
                    selectedFilter = ProviderFilterType.ALL,
                    availableFilters = persistentListOf(
                        ProviderFilterType.ALL,
                        ProviderFilterType.CEX,
                        ProviderFilterType.DEX,
                    ),
                ),
                onProviderClick = {},
                onFilterSelect = {},
            )
        }
    }
}

private class ProviderChooseCryptoPreviewProvider : PreviewParameterProvider<SwapChooseProviderBottomSheetContent> {
    override val values: Sequence<SwapChooseProviderBottomSheetContent>
        get() = sequenceOf(
            SwapChooseProviderContentPreview.state,
        )
}
// endregion