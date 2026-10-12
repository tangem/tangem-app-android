package com.tangem.feature.swap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.reportMeasuredHeight
import com.tangem.core.ui.res.TangemTheme

/**
 * Renders one swap sheet inside a [TangemModal]. The card material never changes while the modal is shown: a glass
 * sheet is a separate modal stacked above the flat one.
 *
 * The modal sizes its card from the content's intrinsic height, so contents must not contain a `SubcomposeLayout`
 * (lazy lists, `BoxWithConstraints`, scaffolds, `EllipsisText`).
 */
@Composable
internal fun SwapBottomSheet(
    content: ComposableModularContentComponent,
    isMaterial: Boolean,
    scrollableContent: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        isMaterialEnabled = isMaterial,
        // Glass sheets keep the modal's own opaque fill: it shows once the card expands (e.g. with the keyboard
        // up), and a translucent token there would be drawn at full alpha.
        containerColor = if (isMaterial) null else TangemTheme.colors3.bg.secondary,
        onBack = onBack,
        scrollableContent = scrollableContent,
        title = { content.Title() },
        content = {
            if (scrollableContent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .reportMeasuredHeight(),
                ) {
                    content.Content(modifier = Modifier.fillMaxWidth())
                    content.Footer()
                }
            } else {
                PinnedFooterContent(content = content)
            }
        },
    )
}

/**
 * Pins the footer to the bottom of the card — right above the keyboard once a focused field expands it — while the
 * content scrolls in the space left above it, so the footer stays reachable on short screens. The scroller is the
 * weighted child: a weight inside a scroll container would get no space at all. Only the content reports its
 * measured height, and at rest the card is exactly content plus footer tall, so the scroller takes no extra room.
 */
@Composable
private fun PinnedFooterContent(content: ComposableModularContentComponent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .reportMeasuredHeight(),
            ) {
                content.Content(modifier = Modifier.fillMaxWidth())
            }
        }
        content.Footer()
    }
}