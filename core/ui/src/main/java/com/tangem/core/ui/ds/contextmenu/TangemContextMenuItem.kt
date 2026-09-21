package com.tangem.core.ui.ds.contextmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.PopUpMenuTestTags

/**
 * Plain row of [TangemContextMenu]: an optional leading [icon] and a [title]. [isDestructive] tints the row with the
 * error color; a disabled row is muted and ignores taps.
 */
@Composable
fun TangemContextMenuItem(
    title: TextReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    isEnabled: Boolean = true,
) {
    val textColor = when {
        !isEnabled -> TangemTheme.colors3.text.tertiary
        isDestructive -> TangemTheme.colors3.text.status.error
        else -> TangemTheme.colors3.text.primary
    }
    val iconTint = when {
        !isEnabled -> TangemTheme.colors3.icon.tertiary
        isDestructive -> TangemTheme.colors3.icon.status.error
        else -> TangemTheme.colors3.icon.primary
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(min = MIN_WIDTH)
            .clickableSingle(onClick = onClick, enabled = isEnabled)
            .padding(vertical = TangemTheme.dimens2.x2_5, horizontal = TangemTheme.dimens2.x4)
            .testTag(PopUpMenuTestTags.BUTTON),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(TangemTheme.dimens2.x6),
            )
        }
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val MIN_WIDTH = 238.dp