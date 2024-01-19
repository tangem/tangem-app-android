package com.tangem.managetokens.presentation.managetokens.ui

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState

@Composable
internal fun ChooseNetworkBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<ChooseNetworkBottomSheetConfig>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) {
        ChooseNetworkScreen(state = it.selectedToken, walletState = it.chooseWalletState)
    }
}

internal class ChooseNetworkBottomSheetConfig(
    val selectedToken: TokenItemState.Loaded,
    val chooseWalletState: ChooseWalletState,
) : TangemBottomSheetConfigContent