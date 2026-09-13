package com.tangem.features.hotwallet.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme

private const val DISABLED_COLORS_ALPHA = 0.5f

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
internal fun OptionBlock(
    title: String,
    description: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TangemTheme.colors.background.primary,
    badge: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
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
        trailingContent?.invoke()
    }
}

@Composable
internal fun ChevronIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right_24),
        tint = TangemTheme.colors.icon.informative,
        contentDescription = null,
    )
}

@Composable
internal fun LoaderIcon() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = TangemTheme.colors.icon.informative,
    )
}