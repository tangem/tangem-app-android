package com.tangem.features.markets.portfolio.impl.ui

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
import com.tangem.core.ui.components.appbar.TangemTopAppBarHeight
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewAddToPortfolioBSContentProvider
import com.tangem.features.markets.portfolio.impl.ui.state.WalletSelectorBSContentUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WalletSelectorBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<WalletSelectorBSContentUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        addBottomInsets = false,
        title = { content ->
            TangemTopAppBar(
                title = resourceReference(R.string.manage_tokens_wallet_selector_title),
                titleAlignment = Alignment.CenterHorizontally,
                startButton = TopAppBarButtonUM.Back(content.onBack),
                height = TangemTopAppBarHeight.BOTTOM_SHEET,
            )
        },
    ) { content ->
        Content(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = TangemTheme.dimens.spacing16,
                    vertical = TangemTheme.dimens.spacing8,
                ),
            state = content,
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
private fun Preview() {
    TangemThemePreview {
        WalletSelectorBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = WalletSelectorBSContentUM(
                    userWallets = persistentListOf(
                        PreviewAddToPortfolioBSContentProvider().userWallet.copy(
                            endIcon = UserWalletItemUM.EndIcon.None,
                        ),
                        PreviewAddToPortfolioBSContentProvider().userWallet.copy(
                            endIcon = UserWalletItemUM.EndIcon.Checkmark,
                        ),
                        PreviewAddToPortfolioBSContentProvider().userWallet.copy(
                            endIcon = UserWalletItemUM.EndIcon.None,
                        ),
                    ),
                    onBack = {},
                ),
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewContent() {
    TangemThemePreview {
        Content(
            state = WalletSelectorBSContentUM(
                userWallets = persistentListOf(
                    PreviewAddToPortfolioBSContentProvider().userWallet.copy(
                        endIcon = UserWalletItemUM.EndIcon.None,
                    ),
                    PreviewAddToPortfolioBSContentProvider().userWallet.copy(
                        endIcon = UserWalletItemUM.EndIcon.Checkmark,
                    ),
                    PreviewAddToPortfolioBSContentProvider().userWallet.copy(
                        endIcon = UserWalletItemUM.EndIcon.None,
                    ),
                ),
                onBack = {},
            ),
        )
    }
}