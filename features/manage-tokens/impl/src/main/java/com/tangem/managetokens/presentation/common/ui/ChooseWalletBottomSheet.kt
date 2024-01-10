package com.tangem.managetokens.presentation.common.ui

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.managetokens.presentation.common.state.ChooseWalletState

@Composable
internal fun ChooseWalletBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<ChooseWalletBottomSheetConfig>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) {
        ChooseWalletScreen(state = it.chooseWalletState)
    }
}

internal class ChooseWalletBottomSheetConfig(
    val chooseWalletState: ChooseWalletState.Choose,
) : TangemBottomSheetConfigContent