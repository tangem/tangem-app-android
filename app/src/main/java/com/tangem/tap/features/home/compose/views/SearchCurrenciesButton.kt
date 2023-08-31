package com.tangem.tap.features.home.compose.views

import androidx.compose.material.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonColors
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.wallet.R

@Composable
internal fun SearchCurrenciesButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        text = stringResource(id = R.string.common_search_tokens),
        icon = TangemButtonIconPosition.Start(R.drawable.ic_search_24),
        onClick = onClick,
        colors = SearchCurrenciesButtonColors,
        showProgress = false,
        enabled = true,
    )
}

private val SearchCurrenciesButtonColors: ButtonColors = TangemButtonColors(
    backgroundColor = TangemColorPalette.Light2,
    contentColor = TangemColorPalette.Dark6,
    disabledBackgroundColor = TangemColorPalette.Light2,
    disabledContentColor = TangemColorPalette.Dark6,
)