package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun InformationTextBlock(
    text: TextReference,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null,
    textColor: Color = TangemTheme.colors2.text.neutral.tertiary,
    infoIconColor: Color = TangemTheme.colors2.markers.iconGray,
    informationTextBlockIconPosition: InformationTextBlockIconPosition = InformationTextBlockIconPosition.START,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val infoIcon: @Composable () -> Unit = {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x4),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_information_24),
            tint = infoIconColor,
            contentDescription = null,
        )
    }

    val contentText: @Composable () -> Unit = {
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography2.captionSemibold12,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = modifier
            .conditional(
                condition = onInfoClick != null,
                modifier = {
                    onInfoClick?.let { infoClick ->
                        clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = infoClick,
                        )
                    } ?: this
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        when (informationTextBlockIconPosition) {
            InformationTextBlockIconPosition.START -> {
                infoIcon()
                contentText()
            }
            InformationTextBlockIconPosition.END -> {
                contentText()
                infoIcon()
            }
        }
    }
}

internal enum class InformationTextBlockIconPosition {
    START, END
}