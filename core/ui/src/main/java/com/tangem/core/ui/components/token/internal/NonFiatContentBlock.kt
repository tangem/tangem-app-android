package com.tangem.core.ui.components.token.internal

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.R
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@Composable
internal fun NonFiatContentBlock(
    state: TokenItemState,
    reorderableTokenListState: ReorderableLazyListState?,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        label = "Update non content fiat block",
        modifier = modifier,
        contentKey = { it::class.java },
    ) { animatedState ->
        when (animatedState) {
            is TokenItemState.Draggable -> DraggableImage(reorderableTokenListState = reorderableTokenListState)
            is TokenItemState.Unreachable -> NonFiatContentText(text = R.string.common_unreachable)
            is TokenItemState.NoAddress -> NonFiatContentText(text = R.string.common_no_address)
            is TokenItemState.Content,
            is TokenItemState.Loading,
            is TokenItemState.Locked,
            -> Unit
        }
    }
}

@Composable
private fun DraggableImage(reorderableTokenListState: ReorderableLazyListState?) {
    Box(
        modifier = Modifier
            .size(size = TangemTheme.dimens.size32)
            .then(
                other = if (reorderableTokenListState != null) {
                    Modifier.detectReorder(reorderableTokenListState)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_drag_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
    }
}

@Composable
private fun NonFiatContentText(@StringRes text: Int) {
    Text(
        text = stringResourceSafe(id = text),
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography.body2,
    )
}