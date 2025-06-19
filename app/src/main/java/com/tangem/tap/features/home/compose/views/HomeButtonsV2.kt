package com.tangem.tap.features.home.compose.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TestTags
import com.tangem.wallet.R

@Composable
internal fun HomeButtonsV2(
    btnScanStateInProgress: Boolean,
    onScanButtonClick: () -> Unit,
    onCreateNewWalletButtonClick: () -> Unit,
    onAddExistingWalletButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CreateNewWalletButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.STORIES_SCREEN_CREATE_NEW_WALLET_BUTTON),
            onClick = onCreateNewWalletButtonClick,
        )
        AddExistingWalletButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.STORIES_SCREEN_ADD_EXISTING_WALLET_BUTTON),
            onClick = onAddExistingWalletButtonClick,
        )
        ScanCardButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.STORIES_SCREEN_SCAN_BUTTON),
            showProgress = btnScanStateInProgress,
            onClick = onScanButtonClick,
        )
    }
}

@Composable
private fun CreateNewWalletButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    StoriesButton(
        modifier = modifier,
        text = stringResourceSafe(id = R.string.home_button_create_new_wallet),
        useDarkerColors = false,
        onClick = onClick,
    )
}

@Composable
private fun AddExistingWalletButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    StoriesButton(
        modifier = modifier,
        text = stringResourceSafe(id = R.string.home_button_add_existing_wallet),
        useDarkerColors = true,
        onClick = onClick,
    )
}

@Composable
private fun ScanCardButton(showProgress: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StoriesButton(
        modifier = modifier,
        text = stringResourceSafe(id = R.string.home_button_scan),
        useDarkerColors = true,
        icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_tangem_24),
        onClick = onClick,
        showProgress = showProgress,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HomeButtonsV2Preview(@PreviewParameter(HomeButtonsV2ParameterProvider::class) state: HomeButtonsV2State) {
    TangemThemePreview {
        Box(
            modifier = Modifier.background(Color.Black),
        ) {
            HomeButtonsV2(
                btnScanStateInProgress = state.btnScanStateInProgress,
                onCreateNewWalletButtonClick = {},
                onAddExistingWalletButtonClick = {},
                onScanButtonClick = {},
                modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
            )
        }
    }
}

private class HomeButtonsV2ParameterProvider : CollectionPreviewParameterProvider<HomeButtonsV2State>(
    collection = listOf(
        HomeButtonsV2State(
            btnScanStateInProgress = false,
        ),
        HomeButtonsV2State(
            btnScanStateInProgress = true,
        ),
    ),
)

private data class HomeButtonsV2State(
    val btnScanStateInProgress: Boolean,
)
// endregion Preview