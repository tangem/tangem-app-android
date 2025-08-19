package com.tangem.features.hotwallet.createwalletbackup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.tangem.features.hotwallet.createwalletbackup.routing.CreateWalletBackupRoute
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent

@Composable
internal fun CreateWalletBackupContent(
    stackState: ChildStack<CreateWalletBackupRoute, ComposableContentComponent>,
    stepperComponent: HotWalletStepperComponent?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
    ) {
        stepperComponent?.Content(Modifier)

        Children(
            stack = stackState,
            animation = stackAnimation(slide()),
            modifier = Modifier.fillMaxSize(),
        ) {
            it.instance.Content(Modifier.fillMaxSize())
        }
    }
}