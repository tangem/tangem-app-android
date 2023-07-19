package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsTopAppBar
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenInfoBlock

@Composable
internal fun TokenDetailsScreen(state: TokenDetailsState) {
    Scaffold(
        topBar = { TokenDetailsTopAppBar(config = state.topAppBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        Column(
            modifier = Modifier
                .padding(paddingValues = scaffoldPaddings)
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            TokenInfoBlock(state = state.tokenInfoBlockState)
            TokenDetailsBalanceBlock(state = state.tokenBalanceBlockState)
        }
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsScreen_LightTheme() {
    TangemTheme(isDark = false) {
        TokenDetailsScreen(state = TokenDetailsPreviewData.tokenDetailsState)
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsScreen_DarkTheme() {
    TangemTheme(isDark = true) {
        TokenDetailsScreen(state = TokenDetailsPreviewData.tokenDetailsState)
    }
}
