package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.shtorka.TangemShtorka
import com.tangem.core.ui.res.TangemTheme

/** Height of the whole sheet header — exactly what the collapsed shtorka shows. */
internal val ShtorkaSheetHeaderHeight = 76.dp

private val DragTipTopPadding = 4.dp
private val DragTipHeight = 4.dp

/** Gap between the drag tip and the content below it. */
private val DragTipBottomPadding = 8.dp

/**
 * Sheet header chrome of the Shtorka 2.0: a small drag tip pinned to the top edge and the
 * feed-provided [content] (search bar or the active feed screen's navbar) below it, so the collapsed
 * card reads as one solid capsule.
 *
 * The tip and the gap under it are laid out explicitly rather than by centring the content, so the
 * distance between the tip and the search field does not drift with the content's height.
 *
 * Rendered in [TangemShtorka]'s `header` slot with the built-in drag handle disabled
 * (`showDragHandle = false`) — the tip here replaces it.
 */
@Composable
internal fun ShtorkaSheetHeader(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(ShtorkaSheetHeaderHeight)
            .padding(top = DragTipTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DragTip()
        SpacerH(DragTipBottomPadding)
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/** Small grabber pill replacing the DS shtorka's built-in drag handle. */
@Composable
private fun DragTip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 32.dp, height = DragTipHeight)
            .background(color = TangemTheme.colors3.icon.tertiary, shape = CircleShape),
    )
}