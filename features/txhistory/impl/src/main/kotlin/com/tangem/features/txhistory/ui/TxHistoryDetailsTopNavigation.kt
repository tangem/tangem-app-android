package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dots_horizontal_20
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM

/**
 * Shared top navigation ("Nav bar") for the transaction details bottom sheet, common to all transaction types.
 *
 * Built on the redesigned [TangemTopNavigation]: a leading status-tinted action icon ([StatusActionIcon]) in the start
 * slot, a status-colored [title][TxHistoryDetailsUM.HeaderUM.title] over a date subtitle in the center slot, and the
 * trailing context-menu (`•••`, grouped in a Material pill) + close (`✕`) buttons in the end slots.
 *
 * Three visual states are driven by [TxHistoryDetailsUM.HeaderUM.status]: the action-icon circle background, the icon
 * tint and the title color change between in-progress (brand/blue), confirmed (neutral) and failed (red). The icon
 * glyph itself is kept as-is on failure — only recolored.
 *
 * Hosted inside a modal bottom sheet on a solid `colors3.bg.secondary` surface, so [WindowInsets] is zeroed (no
 * status-bar reservation) and the top fade/blur background is disabled (`fadeBackground = false`) — otherwise the
 * gradient would show as a stray band over the sheet.
 */
@Composable
internal fun TxHistoryDetailsTopNavigation(
    header: TxHistoryDetailsUM.HeaderUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemTopNavigation(
        modifier = modifier.padding(top = 8.dp),
        windowInsets = WindowInsets(0),
        fadeBackground = false,
        startButton = { StatusActionIcon(iconRes = header.iconRes, status = header.status) },
        endButtonsGroup = {
            // Context menu. Click handling is intentionally not wired yet.
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                iconStart = TangemIconUM.Icon(Icons.ic_dots_horizontal_20),
                contentDescription = resourceReference(R.string.common_more).resolveReference(),
                onClick = {},
            )
        },
        endButton = { TangemButton.Close(onClick = onCloseClick) },
        contentColumn = {
            Text(
                text = header.title.resolveReference(),
                color = header.status.titleColor,
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = header.subtitle.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
    )
}

@Composable
private fun StatusActionIcon(iconRes: Int, status: Status, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(status.circleBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = status.iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

// region Status -> colors3 tokens (three states)

private val Status.circleBackground: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors3.bg.tertiary
        is Status.Unconfirmed -> TangemTheme.colors3.bg.status.infoSubtle
        is Status.Failed -> TangemTheme.colors3.bg.status.errorSubtle
    }

private val Status.iconTint: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors3.icon.primary
        is Status.Unconfirmed -> TangemTheme.colors3.icon.accent.blue
        is Status.Failed -> TangemTheme.colors3.icon.accent.red
    }

private val Status.titleColor: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors3.text.primary
        is Status.Unconfirmed -> TangemTheme.colors3.text.brand
        is Status.Failed -> TangemTheme.colors3.text.status.error
    }

// endregion

// region Preview

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TxHistoryDetailsTopNavigationPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TxHistoryDetailsTopNavigation(
                header = previewHeader(Status.Unconfirmed, stringReference("Swapping")),
                onCloseClick = {},
            )
            TxHistoryDetailsTopNavigation(
                header = previewHeader(Status.Confirmed, stringReference("Swapped")),
                onCloseClick = {},
            )
            TxHistoryDetailsTopNavigation(
                header = previewHeader(Status.Failed, stringReference("Swapping failed")),
                onCloseClick = {},
            )
        }
    }
}

private fun previewHeader(status: Status, title: TextReference) = TxHistoryDetailsUM.HeaderUM(
    iconRes = R.drawable.ic_exchange_vertical_24,
    status = status,
    title = title,
    subtitle = stringReference("Jan 20 2026, 9:24 PM"),
)

// endregion