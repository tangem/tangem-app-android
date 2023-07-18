package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.features.tokendetails.impl.R
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TokenDetailsTopAppBar(config: TokenDetailsTopAppBarConfig) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = config.onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = "Back",
                )
            }
        },
        title = {},
        actions = {
            IconButton(onClick = config.onMoreClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_vertical_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = "More",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    )
}

@Preview
@Composable
private fun Preview_TokenDetailsTopAppBar_LightTheme() {
    TangemTheme(isDark = false) {
        TokenDetailsTopAppBar(config = TokenDetailsPreviewData.tokenDetailsTopAppBarConfig)
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsTopAppBar_DarkTheme() {
    TangemTheme(isDark = true) {
        TokenDetailsTopAppBar(config = TokenDetailsPreviewData.tokenDetailsTopAppBarConfig)
    }
}
