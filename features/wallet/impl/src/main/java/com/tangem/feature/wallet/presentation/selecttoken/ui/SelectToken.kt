package com.tangem.feature.wallet.presentation.selecttoken.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.tokenlist.TokenListComponent

@Composable
internal fun SelectToken(
    @StringRes titleResId: Int,
    onBackClick: () -> Unit,
    tokenListComponent: TokenListComponent,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .imePadding()
            .systemBarsPadding(),
    ) {
        AppBarWithBackButton(
            onBackClick = onBackClick,
            text = stringResource(id = titleResId),
            iconRes = R.drawable.ic_close_24,
        )

        tokenListComponent.Content(
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            modifier = Modifier,
        )
    }
}