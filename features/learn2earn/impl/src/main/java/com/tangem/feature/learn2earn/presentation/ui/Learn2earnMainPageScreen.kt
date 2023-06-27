package com.tangem.feature.learn2earn.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.learn2earn.presentation.ui.state.Learn2earnState

/**
* [REDACTED_AUTHOR]
 */
@Composable
fun Learn2earnMainPageScreen(state: Learn2earnState, modifier: Modifier = Modifier) {
    GetBonusView(
        state = state.mainScreenState,
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.size16)
            .padding(vertical = TangemTheme.dimens.size8),
    )
    Learn2earnDialogs(state.mainScreenState.dialog)
}
