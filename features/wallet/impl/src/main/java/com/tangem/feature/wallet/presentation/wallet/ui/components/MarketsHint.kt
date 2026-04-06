package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.wallet.impl.R

@Composable
internal fun MarketsHint(isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        ) {
            Text(
                text = stringResourceSafe(R.string.markets_hint_part_one),
                style = TangemTheme.typography2.bodyRegular15,
                color = TangemTheme.colors2.text.neutral.primary,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1)) {
                Text(
                    text = stringResourceSafe(R.string.markets_hint_part_two),
                    style = TangemTheme.typography2.bodyRegular15,
                    color = TangemTheme.colors2.text.neutral.tertiary,
                    textAlign = TextAlign.Center,
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_magic_default_24),
                    tint = TangemTheme.colors2.text.neutral.tertiary,
                    contentDescription = null,
                    modifier = Modifier.size(TangemTheme.dimens2.x5),
                )
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MarketsHint_Preview() {
    TangemThemePreviewRedesign {
        MarketsHint(
            isVisible = true,
        )
    }
}
// endregion