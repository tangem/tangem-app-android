package com.tangem.features.earn.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.product.api.navigation.EarnRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultEarnEntryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: EarnEntryComponent.Params,
) : EarnEntryComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<EarnRoute>()

    private val childStack = childStack(
        key = "earnEntryStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = params.start.toInitialRoute(),
        handleBackButton = true,
        childFactory = { _, _ -> EmptyComponent },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStackValue by childStack.subscribeAsState()

        Children(
            stack = childStackValue,
            modifier = modifier,
            animation = stackAnimation { fade() },
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }
    }

    private object EmptyComponent : ComposableContentComponent {
        @Composable
        override fun Content(modifier: Modifier) {
            Box(modifier = modifier)
        }
    }

    @AssistedFactory
    interface Factory : EarnEntryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: EarnEntryComponent.Params,
        ): DefaultEarnEntryComponent
    }
}