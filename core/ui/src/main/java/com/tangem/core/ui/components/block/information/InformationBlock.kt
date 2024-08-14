package com.tangem.core.ui.components.block.information

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.text.TooltipText
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf

@Immutable
class InformationBlockContentScope(val scope: BoxScope) : BoxScope by scope

@Composable
fun InformationBlock(
    title: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable BoxScope.() -> Unit)? = null,
    content: (@Composable InformationBlockContentScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(color = TangemTheme.colors.background.action),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size40)
                .padding(
                    top = TangemTheme.dimens.spacing12,
                    bottom = TangemTheme.dimens.spacing6,
                )
                .padding(horizontal = TangemTheme.dimens.spacing12),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .heightIn(min = TangemTheme.dimens.size20),
                contentAlignment = Alignment.CenterStart,
                content = title,
            )
            if (action != null) {
                Spacer(modifier = Modifier.size(TangemTheme.dimens.spacing8))
                Box(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .heightIn(min = TangemTheme.dimens.size24),
                    contentAlignment = Alignment.CenterEnd,
                    content = action,
                )
            }
        }

        if (content != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .fillMaxWidth(),
            ) {
                val scope = InformationBlockContentScope(scope = this)
                content(scope)
            }
        }
    }
}

// region Previews
@Composable
@Preview(showBackground = true, widthDp = 328)
@Preview(showBackground = true, widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_Grid() {
    TangemThemePreview {
        InformationBlock(
            title = {
                TooltipText(
                    text = stringReference("Grid title"),
                    onInfoClick = { },
                )
            },
            content = {
                GridItems(
                    items = persistentListOf(
                        stringReference("Fist item"),
                        stringReference("Second item"),
                    ),
                    itemContent = {
                        PreviewItem(text = it)
                    },
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 328)
@Preview(showBackground = true, widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_List() {
    TangemThemePreview {
        InformationBlock(
            title = {
                Text(
                    text = "List",
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                )
            },
            action = {
                SecondarySmallButton(
                    config = SmallButtonConfig(
                        text = stringReference("Add token"),
                        icon = TangemButtonIconPosition.Start(R.drawable.ic_plus_24),
                        onClick = {},
                    ),
                )
            },
            content = {
                ListItems(
                    items = persistentListOf(
                        stringReference("Fist item"),
                        stringReference("Second item"),
                    ),
                    itemContent = {
                        PreviewItem(it)
                    },
                    verticalArragement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 328)
@Preview(showBackground = true, widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_Plain() {
    TangemThemePreview {
        InformationBlock(
            title = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
                ) {
                    TooltipText(
                        text = stringReference("Title"),
                        onInfoClick = { },
                    )

                    Text(
                        text = "Subtitle",
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            },
            action = {
                PreviewItem(text = stringReference("Action"))
            },
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 328)
@Preview(showBackground = true, widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_Tree() {
    TangemThemePreview {
        InformationBlock(
            title = {
                TooltipText(
                    text = stringReference("Tree title"),
                    onInfoClick = { },
                )
            },
            content = {
                ArrowRowItems(
                    itemPadding = PaddingValues(vertical = TangemTheme.dimens.spacing4),
                    items = persistentListOf(
                        stringReference("Fist item"),
                        stringReference("Second item"),
                        stringReference("Third item"),
                    ),
                    rootContent = {
                        PreviewItem(stringReference("Root"))
                    },
                    itemContent = {
                        PreviewItem(it)
                    },
                )
            },
        )
    }
}

@Composable
private fun PreviewItem(text: TextReference) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TangemTheme.colors.background.secondary)
            .padding(all = TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = text.resolveReference(),
            color = TangemTheme.colors.text.primary1,
        )
    }
}
// endregion