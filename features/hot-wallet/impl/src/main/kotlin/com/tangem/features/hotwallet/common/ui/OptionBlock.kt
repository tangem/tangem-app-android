package com.tangem.features.hotwallet.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme

private const val DISABLED_COLORS_ALPHA = 0.5f

@Suppress("LongParameterList")
@Composable
internal fun OptionBlock(
    title: String,
    description: String,
    badge: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .alpha(if (enabled) 1f else DISABLED_COLORS_ALPHA)
            .background(
                color = backgroundColor,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .conditional(onClick != null && enabled) {
                onClick?.let { clickableSingle(onClick = it) } ?: Modifier
            }
            .padding(16.dp),
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 4.dp),
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            badge?.invoke()
        }
        Text(
            modifier = Modifier
                .padding(top = 4.dp),
            text = description,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}