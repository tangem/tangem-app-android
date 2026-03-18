package com.tangem.core.ui.ds.row.internal

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.OrganizeTokensScreenTestTags
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@Composable
internal fun TangemRowTail(
    tangemRowTailUM: TangemRowTailUM,
    modifier: Modifier = Modifier,
    reorderableState: ReorderableLazyListState? = null,
) {
    AnimatedContent(
        targetState = tangemRowTailUM,
        label = "Update non content fiat block",
        modifier = modifier.height(TangemTheme.dimens2.x4),
        contentKey = { it::class.java },
    ) { animatedState ->
        val innerModifier = Modifier.padding(start = TangemTheme.dimens2.x2)
        when (animatedState) {
            TangemRowTailUM.Empty -> Unit
            is TangemRowTailUM.Draggable -> DraggableImage(
                iconRes = animatedState.iconRes,
                reorderableState = reorderableState,
                modifier = innerModifier,
            )
            is TangemRowTailUM.Text -> ContentText(text = animatedState.text, modifier = innerModifier)
            is TangemRowTailUM.Icon -> Icon(
                imageVector = ImageVector.vectorResource(animatedState.iconRes),
                contentDescription = null,
                modifier = modifier,
                tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
            )
        }
    }
}

@Composable
private fun DraggableImage(
    @DrawableRes iconRes: Int,
    reorderableState: ReorderableLazyListState?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size = TangemTheme.dimens2.x6)
            .then(
                other = if (reorderableState != null) {
                    Modifier.detectReorder(reorderableState)
                } else {
                    Modifier
                },
            )
            .testTag(OrganizeTokensScreenTestTags.DRAGGABLE_IMAGE),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
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