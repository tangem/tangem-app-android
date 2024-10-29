package com.tangem.features.onramp.selecttoken.ui

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
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent

@Composable
internal fun OnrampSelectToken(
    @StringRes titleResId: Int,
    onBackClick: () -> Unit,
    onrampTokenListComponent: OnrampTokenListComponent,
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

        onrampTokenListComponent.Content(
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            modifier = Modifier,
        )
    }
}