package com.tangem.tap.features.welcome.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.tap.features.welcome.component.WelcomeComponent
import com.tangem.tap.features.welcome.ui.WelcomeScreenState
import com.tangem.tap.features.welcome.ui.components.WelcomeScreen

internal class PreviewWelcomeComponent(
    private val initialState: WelcomeScreenState = WelcomeScreenState(),
) : WelcomeComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        WelcomeScreen(
            modifier = modifier,
            state = initialState,
        )
    }
}