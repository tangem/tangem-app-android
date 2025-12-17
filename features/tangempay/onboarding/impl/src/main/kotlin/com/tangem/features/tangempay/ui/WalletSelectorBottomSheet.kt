package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.onboarding.impl.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WalletSelectorBottomSheet(state: WalletSelectorBSContentUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        containerColor = TangemTheme.colors.background.tertiary,
        title = { content ->
            TangemTopAppBar(
                title = resourceReference(R.string.common_choose_wallet),
                titleAlignment = Alignment.CenterHorizontally,
                endButton = TopAppBarButtonUM.Close(onCloseClick = state.onDismiss),
            )
        },
    ) { content ->
        Content(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TangemTheme.dimens.spacing16,
                    vertical = TangemTheme.dimens.spacing8,
                ),
            state = state,
        )
    }
}

@Composable
private fun Content(state: WalletSelectorBSContentUM, modifier: Modifier = Modifier) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        BlockCard(
            modifier = Modifier.fillMaxSize(),
            colors = TangemBlockCardColors.copy(
                containerColor = TangemTheme.colors.background.action,
                disabledContainerColor = TangemTheme.colors.background.action,
            ),
        ) {
            state.userWallets.forEach { state ->
                key(state.id) {
                    UserWalletItem(
                        modifier = Modifier.fillMaxWidth(),
                        blockColors = TangemBlockCardColors.copy(
                            containerColor = TangemTheme.colors.background.action,
                            disabledContainerColor = TangemTheme.colors.background.action,
                        ),
                        state = state,
                    )
                }
            }
        }
        SpacerH(bottomBarHeight)
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWalletSelectorBottomSheet() {
    TangemThemePreview {
        WalletSelectorBottomSheet(
            state = WalletSelectorBSContentUM(
                userWallets = persistentListOf(
                    UserWalletItemUM(
                        id = "1",
                        name = stringReference("Wallet 1"),
                        information = UserWalletItemUM.Information.Loaded(TextReference.Str("3 cards")),
                        balance = UserWalletItemUM.Balance.Loading,
                        isEnabled = true,
                        endIcon = UserWalletItemUM.EndIcon.None,
                        onClick = {},
                    ),
                    UserWalletItemUM(
                        id = "2",
                        name = stringReference("Wallet 2"),
                        information = UserWalletItemUM.Information.Loaded(TextReference.Str("3 cards")),
                        balance = UserWalletItemUM.Balance.Loading,
                        isEnabled = true,
                        endIcon = UserWalletItemUM.EndIcon.None,
                        onClick = {},
                    ),
                ),
                onDismiss = {},
            ),
        )
    }
}