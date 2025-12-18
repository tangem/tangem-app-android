package com.tangem.features.feed.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.features.feed.components.FeedEntryChildFactory

@Composable
internal fun EntryBottomSheetContent(
    stackState: ChildStack<FeedEntryChildFactory.Child, ComposableModularContentComponent>,
    onHeaderSizeChange: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value

    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AnimatedContent(
                targetState = stackState.active.instance,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    if (coordinates.size.height > 0) {
                        with(density) {
                            onHeaderSizeChange(coordinates.size.height.toDp())
                        }
                    }
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { currentState ->
                currentState.Title()
            }
        },
        content = { contentPadding ->
            AnimatedContent(
                targetState = stackState.active.instance,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { currentState ->
                currentState.Content(modifier = Modifier.padding(contentPadding))
            }
        },
    )
}