package com.tangem.core.ui.components.token.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.token.state.TokenItemState.PromoBannerState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun YieldSupplyPromoBanner(state: PromoBannerState, modifier: Modifier = Modifier) {
    when (state) {
        is PromoBannerState.Content -> YieldSupplyPromoBanner(state = state, modifier = modifier)
        is PromoBannerState.Empty -> Unit
    }
}

@Composable
internal fun YieldSupplyPromoBanner(state: PromoBannerState.Content, modifier: Modifier = Modifier) {
    val bgColor = TangemTheme.colors.control.unchecked
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(color = bgColor, shape = TangemTheme.shapes.roundedCornersXMedium)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(onClick = state.onPromoBannerClick)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_analytics_up_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
                modifier = Modifier
                    .padding(end = TangemTheme.dimens.spacing8)
                    .size(TangemTheme.dimens.size16),
            )
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.secondary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = TangemTheme.dimens.spacing8),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_close_24),
                contentDescription = null,
                tint = TangemTheme.colors.text.secondary,
                modifier = Modifier
                    .size(TangemTheme.dimens.size16)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = { state.onCloseClick() },
                    ),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rectangle_bottom),
                contentDescription = null,
                tint = bgColor,
                modifier = Modifier
                    .size(width = 12.dp, height = 8.dp),
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_YieldSupplyPromoBanner() {
    TangemThemePreview {
        YieldSupplyPromoBanner(
            state = PromoBannerState.Content(
                title = TextReference.Str(value = "Earn up to 5% APY"),
                onPromoBannerClick = {},
                onCloseClick = {},
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}