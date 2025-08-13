package com.tangem.tap.features.home.compose.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.StoriesScreenTestTags
import com.tangem.wallet.R

@Composable
internal fun HomeButtons(
    btnScanStateInProgress: Boolean,
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier,
    ) {
        ScanCardButton(
            modifier = Modifier
                .weight(weight = 1f)
                .testTag(StoriesScreenTestTags.SCAN_BUTTON),
            showProgress = btnScanStateInProgress,
            onClick = onScanButtonClick,
        )
        SpacerW12()
        OrderCardButton(
            modifier = Modifier
                .weight(weight = 1f)
                .testTag(StoriesScreenTestTags.ORDER_BUTTON),
            onClick = onShopButtonClick,
        )
    }
}

@Composable
private fun ScanCardButton(showProgress: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StoriesButton(
        modifier = modifier,
        text = stringResourceSafe(id = R.string.home_button_scan),
        useDarkerColors = false,
        icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_tangem_24),
        onClick = onClick,
        showProgress = showProgress,
    )
}

@Composable
private fun OrderCardButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    StoriesButton(
        modifier = modifier,
        text = stringResourceSafe(id = R.string.home_button_order),
        useDarkerColors = true,
        onClick = onClick,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HomeButtonsPreview(@PreviewParameter(HomeButtonsParameterProvider::class) state: HomeButtonsState) {
    TangemThemePreview {
        Box(
            modifier = Modifier.background(Color.Black),
        ) {
            HomeButtons(
                btnScanStateInProgress = state.btnScanStateInProgress,
                onScanButtonClick = {},
                onShopButtonClick = {},
                modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
            )
        }
    }
}

private class HomeButtonsParameterProvider : CollectionPreviewParameterProvider<HomeButtonsState>(
    collection = listOf(
        HomeButtonsState(
            btnScanStateInProgress = false,
        ),
        HomeButtonsState(
            btnScanStateInProgress = true,
        ),
    ),
)

private data class HomeButtonsState(
    val btnScanStateInProgress: Boolean,
)
// endregion Preview