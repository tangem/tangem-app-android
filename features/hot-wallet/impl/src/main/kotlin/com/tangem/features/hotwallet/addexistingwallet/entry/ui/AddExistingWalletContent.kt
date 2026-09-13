package com.tangem.features.hotwallet.addexistingwallet.entry.ui

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
import com.tangem.features.hotwallet.addexistingwallet.entry.routing.AddExistingWalletRoute
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent

@Composable
internal fun AddExistingWalletContent(
    stackState: ChildStack<AddExistingWalletRoute, ComposableContentComponent>,
    stepperComponent: HotWalletStepperComponent?,
) {
    // The cloud-restore screens are built on the redesigned design system and draw edge to edge: they
    // own their insets and paint their own background, so the container must not cut the system bars
    // off with a differently-coloured strip.
    val isRedesignedChild = stackState.active.configuration is AddExistingWalletRoute.RestoreCloudBackup

    Column(
        modifier = Modifier
            .background(
                color = if (isRedesignedChild) {
                    TangemTheme.colors3.bg.primary
                } else {
                    TangemTheme.colors.background.primary
                },
            )
            .fillMaxSize()
            .then(if (isRedesignedChild) Modifier else Modifier.imePadding().systemBarsPadding()),
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