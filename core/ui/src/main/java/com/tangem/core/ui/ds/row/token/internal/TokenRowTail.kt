package com.tangem.core.ui.ds.row.token.internal

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.OrganizeTokensScreenTestTags
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@Composable
internal fun TokenRowTail(
    tailUM: TangemTokenRowUM.TailUM,
    reorderableTokenListState: ReorderableLazyListState?,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = tailUM,
        label = "Update non content fiat block",
        modifier = modifier,
        contentKey = { it::class.java },
    ) { animatedState ->
        val innerModifier = Modifier.padding(start = TangemTheme.dimens2.x2)
        when (animatedState) {
            TangemTokenRowUM.TailUM.Empty -> Unit
            is TangemTokenRowUM.TailUM.Draggable -> DraggableImage(
                reorderableTokenListState = reorderableTokenListState,
                modifier = innerModifier,
            )
            is TangemTokenRowUM.TailUM.Text -> ContentText(text = animatedState.text, modifier = innerModifier)
        }
    }
}

@Composable
private fun DraggableImage(reorderableTokenListState: ReorderableLazyListState?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size = TangemTheme.dimens2.x6)
            .then(
                other = if (reorderableTokenListState != null) {
                    Modifier.detectReorder(reorderableTokenListState)
                } else {
                    Modifier
                },
            )
            .testTag(OrganizeTokensScreenTestTags.DRAGGABLE_IMAGE),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_drag_24),
            tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
            contentDescription = null,
        )
    }
}

@Composable
private fun ContentText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveAnnotatedReference(),
        color = TangemTheme.colors2.text.neutral.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography2.captionRegular12,
        modifier = modifier,
    )
}