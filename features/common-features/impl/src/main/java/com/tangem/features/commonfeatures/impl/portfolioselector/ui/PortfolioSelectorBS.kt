package com.tangem.features.commonfeatures.impl.portfolioselector.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorUM

@Composable
internal fun PortfolioSelectorBS(
    state: PortfolioSelectorUM,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBack,
        containerColor = TangemTheme.colors3.bg.primary,
        type = TangemBottomSheetType.Modal,
        title = {
            TangemTopNavigation(
                title = state.title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = onDismiss,
            )
        },
        content = {
            PortfolioSelectorContentV2(
                state = state,
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = modifier.padding(horizontal = 16.dp),
            )
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    TangemThemePreview {
        PortfolioSelectorBS(
            state = params,
            onDismiss = {},
            modifier = Modifier,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    TangemThemePreviewRedesign {
        PortfolioSelectorBS(
            state = params,
            onDismiss = {},
            modifier = Modifier,
            onBack = {},
        )
    }
}