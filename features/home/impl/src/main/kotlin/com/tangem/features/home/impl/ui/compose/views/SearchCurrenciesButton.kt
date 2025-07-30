package com.tangem.features.home.impl.ui.compose.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.R

@Composable
internal fun SearchCurrenciesButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    StoriesButton(
        modifier = modifier,
        text = stringResourceSafe(id = R.string.common_search_tokens),
        icon = TangemButtonIconPosition.Start(R.drawable.ic_search_24),
        showProgress = false,
        useDarkerColors = true,
        onClick = onClick,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SearchCurrenciesButtonPreview() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(color = Color.Black)
                .padding(all = TangemTheme.dimens.spacing16),
        ) {
            SearchCurrenciesButton(modifier = Modifier.fillMaxWidth(), onClick = {})
        }
    }
}
// endregion Preview