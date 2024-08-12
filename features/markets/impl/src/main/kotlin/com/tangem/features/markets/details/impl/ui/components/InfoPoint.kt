package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.TooltipText
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.markets.details.impl.ui.state.InfoPointUM

@Composable
internal fun InfoPoint(infoPointUM: InfoPointUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = TangemTheme.dimens.spacing8),
        horizontalAlignment = Alignment.Start,
    ) {
        if (infoPointUM.onInfoClick != null) {
            TooltipText(
                text = infoPointUM.title,
                onInfoClick = infoPointUM.onInfoClick,
                textStyle = TangemTheme.typography.caption2,
            )
        } else {
            Text(
                text = infoPointUM.title.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row {
            Text(
                text = infoPointUM.value,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
            )
            if (infoPointUM.change != null) {
                SpacerW4()
                Icon(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size8)
                        .align(Alignment.CenterVertically),
                    imageVector = ImageVector.vectorResource(
                        id = when (infoPointUM.change) {
                            InfoPointUM.ChangeType.UP -> R.drawable.ic_arrow_up_8
                            InfoPointUM.ChangeType.DOWN -> R.drawable.ic_arrow_down_8
                        },
                    ),
                    tint = when (infoPointUM.change) {
                        InfoPointUM.ChangeType.UP -> TangemTheme.colors.icon.accent
                        InfoPointUM.ChangeType.DOWN -> TangemTheme.colors.icon.warning
                    },
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
internal fun InfoPointShimmer(modifier: Modifier = Modifier, withTooltip: Boolean = false) {
    Column(
        modifier = modifier.padding(vertical = TangemTheme.dimens.spacing8),
        horizontalAlignment = Alignment.Start,
    ) {
        if (withTooltip) {
            Box(
                modifier = Modifier
                    .requiredHeight(TangemTheme.dimens.size16)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                TextShimmer(
                    modifier = Modifier.fillMaxWidth(),
                    style = TangemTheme.typography.caption2,
                    textSizeHeight = false,
                )
            }
        } else {
            TextShimmer(
                modifier = Modifier.fillMaxWidth(),
                style = TangemTheme.typography.caption2,
                textSizeHeight = true,
            )
        }
        TextShimmer(
            modifier = Modifier.fillMaxWidth(fraction = 0.5f),
            style = TangemTheme.typography.body1,
            textSizeHeight = true,
        )
    }
}

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .width(150.dp)
                .background(TangemTheme.colors.background.tertiary),
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

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewShimmer() {
    TangemThemePreview {
        PreviewShimmerContainer(
            shimmerContent = {
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .background(TangemTheme.colors.background.tertiary),
                ) {
                    InfoPointShimmer(modifier = Modifier.fillMaxWidth())
                    InfoPointShimmer(
                        modifier = Modifier.fillMaxWidth(),
                        withTooltip = true,
                    )
                    InfoPointShimmer(
                        modifier = Modifier.fillMaxWidth(),
                        withTooltip = true,
                    )
                    InfoPointShimmer(
                        modifier = Modifier.fillMaxWidth(),
                        withTooltip = true,
                    )
                }
            },
            actualContent = {
                ContentPreview()
            },
        )
    }
}
