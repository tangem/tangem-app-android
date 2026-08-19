package com.tangem.core.ui.components.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
fun BlockItem(model: BlockUM, modifier: Modifier = Modifier, colors: BlockItemColors = TangemBlockItemColors) {
    BlockCard(
        modifier = modifier,
        colors = colors.cardColors,
        onClick = model.onClick,
    ) {
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12, Alignment.Start),
        ) {
            if (model.iconRes != null) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = model.iconRes),
                    tint = when (model.accentType) {
                        BlockUM.AccentType.NONE -> colors.leadingIconColor
                        BlockUM.AccentType.ACCENT -> colors.accentColor
                        BlockUM.AccentType.WARNING -> colors.warningColor
                    },
                    contentDescription = null,
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = model.text.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = when (model.accentType) {
                    BlockUM.AccentType.NONE -> colors.textColor
                    BlockUM.AccentType.ACCENT -> colors.accentColor
                    BlockUM.AccentType.WARNING -> colors.warningColor
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            when (val endContent = model.endContent) {
                is BlockUM.EndContent.None -> Unit
                is BlockUM.EndContent.Icon -> Icon(
                    painter = painterResource(id = endContent.resId),
                    contentDescription = null,
                    tint = when (endContent.accentType) {
                        BlockUM.AccentType.NONE -> colors.textColor
                        BlockUM.AccentType.ACCENT -> colors.accentColor
                        BlockUM.AccentType.WARNING -> colors.warningColor
                    },
                )
                is BlockUM.EndContent.Label -> Label(endContent.label)
            }
        }
    }
}

data class BlockItemColors(
    val cardColors: CardColors,
    val leadingIconColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val warningColor: Color,
)

val TangemBlockItemColors: BlockItemColors
    @Composable
    @ReadOnlyComposable
    get() = BlockItemColors(
        cardColors = TangemBlockCardColors,
        leadingIconColor = TangemTheme.colors.icon.secondary,
        textColor = TangemTheme.colors.text.primary1,
        accentColor = TangemTheme.colors.text.accent,
        warningColor = TangemTheme.colors.text.warning,
    )