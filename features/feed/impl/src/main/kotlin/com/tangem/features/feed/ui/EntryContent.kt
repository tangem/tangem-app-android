package com.tangem.features.feed.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.feed.components.FeedEntryChildFactory

@Composable
internal fun EntryContent(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onHeaderSizeChange: (Dp) -> Unit,
    isOpenedInBottomSheet: Boolean,
) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value

    Surface(contentColor = background) {
        Scaffold(
            containerColor = background,
            contentWindowInsets = WindowInsetsZero,
            topBar = {
                Children(
                    modifier = Modifier
                        .then(
                            if (!isOpenedInBottomSheet) {
                                Modifier.statusBarsPadding()
                            } else {
                                Modifier
                            },
                        )
                        .onGloballyPositioned { coordinates ->
                            if (coordinates.size.height > 0) {
                                with(density) {
                                    onHeaderSizeChange(coordinates.size.height.toDp())
                                }
                            }
                        },
                    stack = stackState.value,
                    animation = stackAnimation(fade()),
                ) { child ->
                    child.instance.Title(bottomSheetState)
                }
            },
            content = { contentPadding ->
                Children(
                    stack = stackState.value,
                    animation = stackAnimation(fade()),
                ) { child ->
                    child.instance.Content(
                        modifier = Modifier.padding(contentPadding),
                        bottomSheetState = bottomSheetState,
                    )
                }
            },
        )
    }
}