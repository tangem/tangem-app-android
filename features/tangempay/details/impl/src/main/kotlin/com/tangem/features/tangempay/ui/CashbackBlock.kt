package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.entity.CashbackBlockUM
import com.tangem.core.ui.R as CoreUiR

/**
 * Cashback block shown on the Payment account screen V2. Renders either a tappable widget with the
 * accrued cashback amount or a warning banner when cashback has been deactivated.
 */
@Composable
internal fun CashbackBlock(state: CashbackBlockUM, modifier: Modifier = Modifier) {
    when (state) {
        is CashbackBlockUM.Widget -> {
            TangemMessage(
                modifier = modifier.clickableSingle(onClick = state.onClick),
                title = state.title,
                subtitle = state.subtitle,
                messageEffect = TangemMessageEffect.Magic,
                trailingContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(CoreUiR.drawable.ic_chevron_right_24),
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
        is CashbackBlockUM.DeactivatedBanner -> {
            TangemMessage(
                modifier = modifier,
                title = resourceReference(CoreUiR.string.tangempay_cashback_deactivated_title),
                subtitle = resourceReference(CoreUiR.string.tangempay_cashback_deactivated_description),
                messageEffect = TangemMessageEffect.Warning,
                buttons = {
                    TangemButton(
                        buttonUM = TangemButtonUM(
                            text = resourceReference(CoreUiR.string.common_got_it),
                            onClick = state.onGotIt,
                            iconPosition = TangemButtonIconPosition.End,
                            size = TangemButtonSize.X9,
                            type = TangemButtonType.Primary,
                            shape = TangemButtonShape.Rounded,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
    }
}

// region preview

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CashbackBlockWidgetPreview() {
    TangemThemePreviewRedesign {
        CashbackBlock(
            state = CashbackBlockUM.Widget(
                title = stringReference("$32.15 cashback in June"),
                subtitle = stringReference("Will be deposited on July 2–5"),
                onClick = {},
            ),
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp)
                .fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CashbackBlockDeactivatedBannerPreview() {
    TangemThemePreviewRedesign {
        CashbackBlock(
            state = CashbackBlockUM.DeactivatedBanner(onGotIt = {}),
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp)
                .fillMaxWidth(),
        )
    }
}

// endregion preview