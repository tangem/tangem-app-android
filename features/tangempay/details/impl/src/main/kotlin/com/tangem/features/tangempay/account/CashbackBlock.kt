package com.tangem.features.tangempay.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.R as CoreUiR

/**
 * Cashback block shown on the Payment account screen. Renders a tappable widget with the accrued
 * cashback amount, a warning banner when cashback has been deactivated, or a tap-to-reload error
 * block when the cashback summary could not be loaded.
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
        is CashbackBlockUM.Error -> {
            TangemMessage(
                modifier = modifier.clickableSingle(onClick = state.onReload),
                title = resourceReference(CoreUiR.string.tangempay_cashback_widget_error_title),
                subtitle = resourceReference(CoreUiR.string.tangempay_cashback_widget_error_description),
                messageEffect = TangemMessageEffect.None,
                leadingContent = {
                    CircledIcon(
                        imageVector = Icons.ic_error_20,
                        iconTint = TangemTheme.colors3.icon.status.error,
                        background = TangemTheme.colors3.bg.status.errorSubtle,
                        size = 40.dp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                },
                trailingContent = {
                    CircledIcon(
                        imageVector = Icons.ic_arrow_refresh_20,
                        iconTint = TangemTheme.colors3.icon.primary,
                        background = TangemTheme.colors3.bg.opaque.primary,
                        size = 32.dp,
                        modifier = Modifier.align(Alignment.CenterVertically),
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

@Composable
private fun CircledIcon(
    imageVector: ImageVector,
    iconTint: Color,
    background: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
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
private fun CashbackBlockErrorPreview() {
    TangemThemePreviewRedesign {
        CashbackBlock(
            state = CashbackBlockUM.Error(onReload = {}),
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