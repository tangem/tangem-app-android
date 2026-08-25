package com.tangem.features.tangempay.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.core.ui.R as CoreUiR

/**
 * Cashback block shown on the Payment account screen. Renders a tappable row card with the accrued
 * cashback amount, a banner when cashback has been deactivated, or a tap-to-reload error row
 * (with an in-flight progress indicator) when the cashback summary could not be loaded.
 */
@Composable
internal fun CashbackBlock(state: CashbackBlockUM, modifier: Modifier = Modifier) {
    when (state) {
        is CashbackBlockUM.Widget -> WidgetRow(state = state, modifier = modifier)
        is CashbackBlockUM.Error -> ErrorRow(state = state, modifier = modifier)
        is CashbackBlockUM.DeactivatedBanner -> DeactivatedBanner(state = state, modifier = modifier)
    }
}

@Composable
private fun WidgetRow(state: CashbackBlockUM.Widget, modifier: Modifier = Modifier) {
    CashbackRow(
        modifier = modifier,
        title = { TangemRowText(text = state.title, role = TangemRowTextRole.Title) },
        subtitle = state.subtitle,
        onClick = state.onClick,
        startIcon = {
            CircledIcon(
                imageVector = Icons.ic_percent_backward_20,
                iconTint = TangemTheme.colors3.icon.status.info,
                background = TangemTheme.colors3.bg.status.infoSubtle,
                size = 40.dp,
            )
        },
        endIcon = {
            CircledIcon(
                imageVector = Icons.ic_chevron_right_20,
                iconTint = TangemTheme.colors3.icon.primary,
                background = TangemTheme.colors3.bg.opaque.primary,
                size = 32.dp,
            )
        },
    )
}

@Composable
private fun ErrorRow(state: CashbackBlockUM.Error, modifier: Modifier = Modifier) {
    CashbackRow(
        modifier = modifier,
        title = {
            Text(
                text = stringResourceSafe(CoreUiR.string.tangempay_cashback_widget_error_title),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitle = resourceReference(CoreUiR.string.tangempay_cashback_widget_error_description),
        onClick = state.onReload.takeUnless { state.isReloading },
        startIcon = {
            CircledIcon(
                imageVector = Icons.ic_error_20,
                iconTint = TangemTheme.colors3.icon.status.error,
                background = TangemTheme.colors3.bg.status.errorSubtle,
                size = 40.dp,
            )
        },
        endIcon = {
            if (state.isReloading) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TangemTheme.colors3.bg.opaque.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    TangemLoader(size = TangemLoaderSize.X20)
                }
            } else {
                CircledIcon(
                    imageVector = Icons.ic_arrow_refresh_20,
                    iconTint = TangemTheme.colors3.icon.primary,
                    background = TangemTheme.colors3.bg.opaque.primary,
                    size = 32.dp,
                )
            }
        },
    )
}

@Composable
private fun CashbackRow(
    subtitle: TextReference?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: @Composable RowScope.() -> Unit,
    startIcon: @Composable BoxScope.() -> Unit,
    endIcon: @Composable BoxScope.() -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.secondary,
        shape = RoundedCornerShape(24.dp),
    ) {
        TangemRow(
            verticalAlignment = TangemRowVerticalAlignment.Center,
            titleSlot = title,
            subtitleSlot = subtitle?.let { { TangemRowText(text = it, role = TangemRowTextRole.Subtitle) } },
            startSlot = startIcon,
            endSlot = endIcon,
            onClick = onClick,
        )
    }
}

@Composable
private fun DeactivatedBanner(state: CashbackBlockUM.DeactivatedBanner, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TangemMessageBanner(
            title = resourceReference(CoreUiR.string.tangempay_cashback_deactivated_title),
            description = resourceReference(CoreUiR.string.tangempay_cashback_deactivated_description),
            showGlowRing = false,
            slotEnd = {
                Icon(
                    imageVector = Icons.ic_error_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                    modifier = Modifier.size(20.dp),
                )
            },
            secondaryButton = TangemMessageBanner.Button(
                text = resourceReference(CoreUiR.string.common_got_it),
                onClick = state.onGotIt,
            ),
        )
        TangemGlowRing(
            modifier = Modifier.matchParentSize(),
            variant = TangemGlowRing.Variant.Error,
            cornerRadius = 28.dp,
        )
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
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CashbackBlock(
                state = CashbackBlockUM.Error(onReload = {}, isReloading = false),
                modifier = Modifier.fillMaxWidth(),
            )
            CashbackBlock(
                state = CashbackBlockUM.Error(onReload = {}, isReloading = true),
                modifier = Modifier.fillMaxWidth(),
            )
        }
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