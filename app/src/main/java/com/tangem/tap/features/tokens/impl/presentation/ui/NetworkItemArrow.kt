package com.tangem.tap.features.tokens.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.wallet.R

private const val SPECIAL_HEIGHT_3_5 = 3.5

/**
* [REDACTED_AUTHOR]
 */
@Composable
internal fun NetworkItemArrow(itemHeight: Dp, isLastItem: Boolean) {
    Box(modifier = Modifier.height(height = itemHeight)) {
        if (!isLastItem) {
            Box(
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing0_5)
                    .background(color = TangemTheme.colors.stroke.primary)
                    .size(width = TangemTheme.dimens.size1, height = itemHeight),
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_link),
            contentDescription = null,
            modifier = Modifier.height(height = itemHeight / 2 + SPECIAL_HEIGHT_3_5.dp),
            tint = TangemTheme.colors.stroke.primary,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NetworkItemArrow_Column() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.primary)
                .padding(start = TangemTheme.dimens.size36),
        ) {
            NetworkItemArrow(itemHeight = TangemTheme.dimens.size62, isLastItem = false)
            NetworkItemArrow(itemHeight = TangemTheme.dimens.size62, isLastItem = true)
        }
    }
}
