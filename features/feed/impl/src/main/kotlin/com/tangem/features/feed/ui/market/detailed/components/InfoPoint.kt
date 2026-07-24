package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUM

@Composable
internal fun InfoPoint(infoPointUM: InfoPointUM, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .conditional(
                condition = infoPointUM.onInfoClick != null,
                modifier = {
                    clickable(
                        interactionSource = interactionSource,
                        onClick = { infoPointUM.onInfoClick?.invoke() },
                    )
                },
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row {
            Text(
                text = infoPointUM.value,
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
            if (infoPointUM.change != null) {
                SpacerW4()
                Icon(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.CenterVertically),
                    imageVector = ImageVector.vectorResource(
                        id = when (infoPointUM.change) {
                            InfoPointUM.ChangeType.UP -> R.drawable.ic_arrow_up_8
                            InfoPointUM.ChangeType.DOWN -> R.drawable.ic_arrow_down_8
                        },
                    ),
                    tint = when (infoPointUM.change) {
                        InfoPointUM.ChangeType.UP -> TangemTheme.colors3.icon.accent.blue
                        InfoPointUM.ChangeType.DOWN -> TangemTheme.colors3.icon.accent.red
                    },
                    contentDescription = null,
                )
            }
        }
        if (infoPointUM.onInfoClick != null) {
            InformationTextBlock(
                text = infoPointUM.title,
                onInfoClick = infoPointUM.onInfoClick,
                informationTextBlockIconPosition = InformationTextBlockIconPosition.START,
            )
        } else {
            Text(
                text = infoPointUM.title.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun InfoPointShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InfoPointShimmerLine(
            style = TangemTheme.typography3.heading.small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp),
        )
        InfoPointShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 68.dp,
        )
    }
}

@Composable
private fun InfoPointShimmerLine(style: TextStyle, modifier: Modifier = Modifier, width: Dp? = null) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .width(150.dp)
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            InfoPoint(
                infoPointUM = InfoPointUM(
                    title = stringReference("Market Cap"),
                    value = "$1,000,000,000",
                ),
            )
            InfoPoint(
                infoPointUM = InfoPointUM(
                    title = stringReference("Market Cap"),
                    value = "$1,000,000,000",
                    onInfoClick = { },
                ),
            )
            InfoPoint(
                infoPointUM = InfoPointUM(
                    title = stringReference("Market Cap"),
                    value = "$1,000,000",
                    change = InfoPointUM.ChangeType.UP,
                    onInfoClick = { },
                ),
            )
            InfoPoint(
                infoPointUM = InfoPointUM(
                    title = stringReference("Market Cap"),
                    value = "$1,000,000",
                    change = InfoPointUM.ChangeType.DOWN,
                    onInfoClick = { },
                ),
            )
        }
    }
}