package com.tangem.features.hotwallet.addexistingwallet.root.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.hotwallet.addexistingwallet.root.routing.AddExistingWalletRoute

@Composable
internal fun AddExistingWalletContent(stackState: ChildStack<AddExistingWalletRoute, ComposableContentComponent>) {
    Children(
        stack = stackState,
        animation = stackAnimation(slide()),
        modifier = Modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
    ) {
        it.instance.Content(Modifier.fillMaxSize())
    }
}