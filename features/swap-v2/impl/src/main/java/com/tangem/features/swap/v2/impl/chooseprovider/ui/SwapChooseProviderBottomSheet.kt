package com.tangem.features.swap.v2.impl.chooseprovider.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.provider.ProviderChooseCrypto
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapChooseProviderBottomSheetContent
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapProviderListItem
import com.tangem.features.swap.v2.impl.chooseprovider.ui.preview.SwapChooseProviderContentPreview
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.ImmutableList

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

@Composable
internal fun SwapChooseProviderContent(
    providerList: ImmutableList<SwapProviderListItem>,
    onProviderClick: (SwapQuoteUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 13.dp),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.onramp_choose_provider_title_hint),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        providerList.fastForEachIndexed { index, provider ->
            ProviderChooseCrypto(
                providerChooseUM = provider.providerUM,
                onClick = { onProviderClick(provider.quote) },
                modifier = modifier
                    .conditional(index == 0) { padding(top = 24.dp) },
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
                providerList = params.providerList,
                onProviderClick = {},
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