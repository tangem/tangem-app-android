package com.tangem.core.ui.components.buttons.chip

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun Chip(text: TextReference, @DrawableRes iconResId: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .defaultMinSize(
                minWidth = TangemTheme.dimens.size46,
                minHeight = TangemTheme.dimens.size28,
            )
            .clip(RoundedCornerShape(size = TangemTheme.dimens.size100))
            .clickable(onClick = onClick)
            .background(color = TangemTheme.colors.background.tertiary)
            .padding(
                horizontal = TangemTheme.dimens.spacing12,
                vertical = TangemTheme.dimens.spacing6,
            ),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(size = TangemTheme.dimens.size16),
            tint = TangemTheme.colors.icon.secondary,
        )

        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Box(Modifier.background(TangemTheme.colors.background.action)) {
            Chip(
                text = stringReference("Chip Chip Chip"),
                iconResId = R.drawable.ic_arrow_down_24,
                onClick = {},
            )
        }
    }
}