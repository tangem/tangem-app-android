package com.tangem.core.ui.components.tokenlist.internal

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.rows.NetworkTitle
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState

/**
 * Group title item
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
fun GroupTitleItem(state: TokensListItemUM.GroupTitle, modifier: Modifier = Modifier) {
    BaseGroupTitleItem(text = state.text, modifier = modifier)
}

/**
 * Draggable network group item
 *
 * @param state                     state
 * @param reorderableTokenListState reorderable token list state
 * @param modifier                  modifier
 */
@Composable
fun DraggableGroupTitleItem(
    state: TokensListItemUM.GroupTitle,
    reorderableTokenListState: ReorderableLazyListState,
    modifier: Modifier = Modifier,
) {
    BaseGroupTitleItem(
        text = state.text,
        modifier = modifier,
        action = { DraggableIcon(reorderableTokenListState = reorderableTokenListState) },
    )
}

@Composable
private fun BaseGroupTitleItem(
    text: TextReference,
    modifier: Modifier = Modifier,
    action: (@Composable BoxScope.() -> Unit)? = null,
) {
    NetworkTitle(
        title = {
            Text(
                text = text.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
        modifier = modifier,
        action = action,
    )
}

@Composable
private fun DraggableIcon(reorderableTokenListState: ReorderableLazyListState) {
    Box(
        modifier = Modifier.detectReorder(reorderableTokenListState),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = rememberVectorPainter(
                image = ImageVector.vectorResource(id = R.drawable.ic_group_drop_24),
            ),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NetworkTitleItemPreview(@PreviewParameter(GroupTitleItemProvider::class) isDraggable: Boolean) {
    TangemThemePreview {
        val state = TokensListItemUM.GroupTitle(
            id = 1,
            text = resourceReference(
                id = R.string.wallet_network_group_title,
                formatArgs = wrappedList(resourceReference(id = R.string.main_tokens)),
            ),
        )

        if (isDraggable) {
            DraggableGroupTitleItem(
                state = state,
                reorderableTokenListState = rememberReorderableLazyListState(onMove = { _, _ -> }),
                modifier = Modifier.background(color = TangemTheme.colors.background.primary),
            )
        } else {
            GroupTitleItem(
                state = state,
                modifier = Modifier.background(color = TangemTheme.colors.background.primary),
            )
        }
    }
}

private object GroupTitleItemProvider : CollectionPreviewParameterProvider<Boolean>(collection = listOf(true, false))