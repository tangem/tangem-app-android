package com.tangem.features.tokenreceive.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes

@Composable
internal fun TokenReceiveContent(
    stackState: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = stackState,
        animation = stackAnimation(fade()),
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(),
    ) {
        it.instance.Content(Modifier.fillMaxSize())
    }
}