package com.tangem.managetokens.presentation.customtokens.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.ui.ChooseWalletScreen

@Composable
internal fun CustomTokensChooseWalletScreen(state: ChooseWalletState.Choose) {
    ChooseWalletScreen(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    )
}