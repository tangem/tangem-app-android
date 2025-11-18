package com.tangem.features.home.impl.ui.compose.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.R

@Composable
internal fun HomeButtonsV2(onGetStartedClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StoriesButton(
            modifier = modifier,
            text = stringResourceSafe(id = R.string.common_get_started),
            useDarkerColors = false,
            onClick = onGetStartedClick,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HomeButtonsV2Preview() {
    TangemThemePreview {
        Box(
            modifier = Modifier.background(Color.Black),
        ) {
            HomeButtonsV2(
                onGetStartedClick = {},
                modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
            )
        }
    }
}
// endregion Preview