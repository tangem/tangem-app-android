package com.tangem.feature.learn2earn.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.learn2earn.presentation.Learn2earnViewModel

/**
[REDACTED_AUTHOR]
 */
@Composable
fun Learn2earnMainPageScreen(viewModel: Learn2earnViewModel, modifier: Modifier = Modifier) {
    GetBonusView(
        state = viewModel.uiState.mainScreenState,
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.size16)
            .padding(vertical = TangemTheme.dimens.size8),
    )
    Learn2earnDialogs(viewModel.uiState.mainScreenState.dialog)
}