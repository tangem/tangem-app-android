package com.tangem.features.hotwallet.common.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) {
            backgroundColor
        } else {
            backgroundColor.copy(alpha = DISABLED_COLORS_ALPHA)
        },
    )
    val titleColor by animateColorAsState(
        targetValue = if (enabled) {
            TangemTheme.colors.text.primary1
        } else {
            TangemTheme.colors.text.primary1.copy(alpha = DISABLED_COLORS_ALPHA)
        },
    )
    val descriptionColor by animateColorAsState(
        targetValue = if (enabled) {
            TangemTheme.colors.text.tertiary
        } else {
            TangemTheme.colors.text.tertiary.copy(alpha = DISABLED_COLORS_ALPHA)
        },
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(
                color = backgroundColor,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .conditional(onClick != null) {
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
                color = titleColor,
            )
            badge?.invoke()
        }
        Text(
            modifier = Modifier
                .padding(top = 4.dp),
            text = description,
            style = TangemTheme.typography.body2,
            color = descriptionColor,
        )
    }
}