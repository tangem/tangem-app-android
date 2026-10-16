package com.tangem.feature.swap.mcp.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_right_20

/**
 * The two currencies of a swap: the one swapped from, an arrow and the one swapped to.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=25973-248232)
 */
@Composable
internal fun ExchangeIntentTokenPair(
    from: TangemTokenIcon.UiState,
    to: TangemTokenIcon.UiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemTokenIcon(state = from, size = TangemTokenIcon.Size.X56)
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.ic_arrow_right_20,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.tertiary,
        )
        TangemTokenIcon(state = to, size = TangemTokenIcon.Size.X56)
    }
}

@Preview(widthDp = 200, showBackground = true)
@Preview(widthDp = 200, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeIntentTokenPairPreview() {
    TangemThemePreviewRedesign {
        ExchangeIntentTokenPair(
            from = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
            to = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
        )
    }
}