package com.tangem.features.onboarding.v2.entry.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute

@Composable
internal inline fun OnboardingEntry(
    modifier: Modifier = Modifier,
    childStack: ChildStack<OnboardingRoute, Any>,
    stepperContent: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(TangemTheme.colors.background.primary),
    ) {
        stepperContent(Modifier.fillMaxWidth())

        Children(
            stack = childStack,
            animation = stackAnimation(slide()),
        ) {
            (it.instance as? ComposableContentComponent)?.Content(modifier = modifier)
        }
    }
}
