package com.tangem.features.nft.details.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@Composable
internal fun NFTDetailsBlocksGroup(
    title: TextReference,
    action: (@Composable BoxScope.() -> Unit)?,
    items: ImmutableList<NFTAssetUM.BlockItem>,
    modifier: Modifier = Modifier,
) {
    val itemsCount = items.size
    if (items.size > 0) {
        val rowCount = (itemsCount + 1) / 2
        val paddingValues = PaddingValues(
            horizontal = TangemTheme.dimens.spacing6,
            vertical = TangemTheme.dimens.spacing8,
        )
        InformationBlock(
            modifier = modifier,
            title = {
                NFTDetailsGroupTitle(
                    text = title,
                )
            },
            action = action,
            contentHorizontalPadding = TangemTheme.dimens.spacing6,
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = TangemTheme.dimens.spacing12),
            ) {
                repeat(rowCount) { rowIndex ->
                    val item1 = items[rowIndex * 2]
                    val item2 = items.getOrNull(rowIndex * 2 + 1)
                    key(rowIndex) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            NFTDetailsGroupBlock(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(paddingValues),
                                title = item1.title,
                                value = stringReference(item1.value),
                                titleEllipsis = item1.titleTextEllipsis,
                                valueEllipsis = item1.valueTextEllipsis,
                                showInfoButton = item1.showInfoButton,
                                onBlockClick = item1.onBlockClick,
                                onValueClick = item1.onValueClick,
                            )
                            if (item2 == null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(paddingValues),
                                )
                            } else {
                                NFTDetailsGroupBlock(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(paddingValues),
                                    title = item2.title,
                                    value = stringReference(item2.value),
                                    titleEllipsis = item2.titleTextEllipsis,
                                    valueEllipsis = item2.valueTextEllipsis,
                                    showInfoButton = item2.showInfoButton,
                                    onBlockClick = item2.onBlockClick,
                                    onValueClick = item2.onValueClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NFTDetailsGroupTitle(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text.resolveReference(),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
internal fun NFTDetailsGroupBlock(
    title: TextReference,
    value: TextReference,
    showInfoButton: Boolean,
    modifier: Modifier = Modifier,
    onBlockClick: (() -> Unit)? = null,
    onValueClick: (() -> Unit)? = null,
    titleEllipsis: TextEllipsis = TextEllipsis.End,
    valueEllipsis: TextEllipsis = TextEllipsis.End,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = onBlockClick != null,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onBlockClick?.invoke()
                },
        ) {
            EllipsisText(
                modifier = Modifier
                    .weight(1f, fill = false),
                text = title.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                ellipsis = titleEllipsis,
            )
            if (showInfoButton) {
                Icon(
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens.spacing4)
                        .size(TangemTheme.dimens.size16),
                    painter = painterResource(R.drawable.ic_information_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                )
            }
        }
        EllipsisText(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = onValueClick != null || onBlockClick != null,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (onValueClick != null) {
                        onValueClick.invoke()
                    } else {
                        onBlockClick?.invoke()
                    }
                },
            text = value.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            ellipsis = valueEllipsis,
        )
    }
}

@Composable
internal fun NFTBlocksGroupAction(
    text: TextReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startIcon: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing2),
    ) {
        startIcon()
        Text(
            text = text.resolveReference(),
            color = TangemTheme.colors.text.accent,
            style = TangemTheme.typography.subtitle2,
        )
    }
}

@Composable
internal fun NFTBlocksGroupActionIcon(@DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier.size(size = TangemTheme.dimens.size16),
        painter = painterResource(id = iconRes),
        tint = TangemTheme.colors.icon.accent,
        contentDescription = null,
    )
}

@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTDetailsInfoBlock(
    @PreviewParameter(NFTAssetBlocksProvider::class) state: ImmutableList<NFTAssetUM.BlockItem>,
) {
    TangemThemePreview {
        Column {
            NFTDetailsBlocksGroup(
                title = stringReference("Title"),
                action = null,
                items = state,
            )
            NFTDetailsBlocksGroup(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
                title = stringReference("Title"),
                action = {
                    NFTBlocksGroupAction(
                        text = resourceReference(R.string.common_see_all),
                        startIcon = { },
                        onClick = { },
                    )
                },
                items = state,
            )
            NFTDetailsBlocksGroup(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
                title = stringReference("Title"),
                action = {
                    NFTBlocksGroupAction(
                        text = resourceReference(R.string.common_explore),
                        startIcon = {
                            NFTBlocksGroupActionIcon(iconRes = R.drawable.ic_compass_24)
                        },
                        onClick = { },
                    )
                },
                items = state,
            )
        }
    }
}

private class NFTAssetBlocksProvider : CollectionPreviewParameterProvider<ImmutableList<NFTAssetUM.BlockItem>>(
    collection = listOf(
        persistentListOf(
            NFTAssetUM.BlockItem(
                title = stringReference("Tier"),
                value = "Infinite",
                showInfoButton = true,
            ),
            NFTAssetUM.BlockItem(
                title = stringReference("Phygital Toy"),
                value = "None",
                showInfoButton = true,
            ),
            NFTAssetUM.BlockItem(
                title = stringReference("Class"),
                value = "CYBER",
                showInfoButton = true,
            ),
            NFTAssetUM.BlockItem(
                title = stringReference("Accessory"),
                value = "No accessory",
                showInfoButton = true,
            ),
            NFTAssetUM.BlockItem(
                title = stringReference("Sneakers"),
                value = "Boots",
                showInfoButton = true,
            ),
        ),
    ),
)