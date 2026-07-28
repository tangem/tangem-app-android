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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUM

@Composable
internal fun InfoPoint(infoPointUM: InfoPointUM, modifier: Modifier = Modifier) {
    InfoPointV2(infoPointUM, modifier)
}

@Composable
private fun InfoPointV2(infoPointUM: InfoPointUM, modifier: Modifier = Modifier) {
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
            .padding(vertical = TangemTheme.dimens.spacing8),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row {
            Text(
                text = infoPointUM.value,
                style = TangemTheme.typography2.headingSemibold20,
                color = TangemTheme.colors2.text.neutral.primary,
            )
            if (infoPointUM.change != null) {
                SpacerW4()
                Icon(
                    modifier = Modifier
                        .size(TangemTheme.dimens2.x2)
                        .align(Alignment.CenterVertically),
                    imageVector = ImageVector.vectorResource(
                        id = when (infoPointUM.change) {
                            InfoPointUM.ChangeType.UP -> R.drawable.ic_arrow_up_8
                            InfoPointUM.ChangeType.DOWN -> R.drawable.ic_arrow_down_8
                        },
                    ),
                    tint = when (infoPointUM.change) {
                        InfoPointUM.ChangeType.UP -> TangemTheme.colors2.markers.iconBlue
                        InfoPointUM.ChangeType.DOWN -> TangemTheme.colors2.markers.iconRed
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
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun InfoPointShimmer(modifier: Modifier = Modifier) {
    InfoPointShimmerV2(modifier)
}

@Composable
private fun InfoPointShimmerV2(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = TangemTheme.dimens2.x6),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        RectangleShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(end = 10.dp),
            radius = TangemTheme.dimens2.x25,
        )

        RectangleShimmer(
            modifier = Modifier
                .width(68.dp)
                .height(16.dp),
            radius = TangemTheme.dimens2.x25,
        )
    }
}

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreviewV2() {
    TangemThemePreviewRedesign {
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