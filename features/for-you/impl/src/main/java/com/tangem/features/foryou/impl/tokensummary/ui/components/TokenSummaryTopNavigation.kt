package com.tangem.features.foryou.impl.tokensummary.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryHeaderUM

/**
 * Top navigation ("Nav bar") for the token summary screen.
 *
 * Built on the redesigned [TangemTopNavigation]: a leading [currency icon][CurrencyIconState] in the start slot, a
 * [title][TokenSummaryHeaderUM.title] over a [subtitle][TokenSummaryHeaderUM.subtitle] in the center slot, and a
 * trailing close (`✕`) button in the end slot. Title and subtitle are single-line and ellipsized on overflow.
 *
 * Hosted inside a modal bottom sheet, so [WindowInsets] is zeroed (no status-bar reservation) and the background blur
 * is disabled.
 *
 * @param header content of the navigation bar — the currency icon, title and subtitle to display.
 * @param onCloseClick invoked when the trailing close button is tapped.
 * @param modifier [Modifier] applied to the root navigation bar.
 */
@Composable
internal fun TokenSummaryTopNavigation(
    header: TokenSummaryHeaderUM,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit,
) {
    TangemTopNavigation(
        modifier = modifier,
        windowInsets = WindowInsets(0),
        blurBackground = false,
        startButton = {
            TangemIcon(
                tangemIconUM = header.tangemIconUM,
                modifier = Modifier.size(40.dp),
            )
        },
        endButton = {
            TangemButton.Close(
                onClick = onCloseClick,
            )
        },
        contentColumn = {
            Text(
                text = header.title.resolveReference(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (header.subtitle != null) {
                Text(
                    text = header.subtitle.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

// region Preview

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryTopNavigationPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TokenSummaryTopNavigation(
                header = previewHeader(
                    title = stringReference("Ethereum"),
                    subtitle = stringReference("ETH"),
                ),
                onCloseClick = {},
            )
            TokenSummaryTopNavigation(
                header = previewHeader(
                    title = stringReference("Ethereum"),
                    subtitle = null,
                ),
                onCloseClick = {},
            )
        }
    }
}

private fun previewHeader(title: TextReference, subtitle: TextReference?) = TokenSummaryHeaderUM(
    tangemIconUM = TangemIconUM.Currency(
        CurrencyIconState.CustomTokenIcon(
            tint = TangemColorPalette.Black,
            background = TangemColorPalette.Meadow,
            topBadgeIconResId = R.drawable.img_polygon_22,
            isGrayscale = false,
        ),
    ),
    title = title,
    subtitle = subtitle,
)

// endregion