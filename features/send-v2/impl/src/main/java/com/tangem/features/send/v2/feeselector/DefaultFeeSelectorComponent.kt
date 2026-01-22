package com.tangem.features.send.v2.feeselector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.extended.FeeExtendedSelectorComponent
import com.tangem.features.send.v2.feeselector.component.speed.FeeSpeedSelectorComponent
import com.tangem.features.send.v2.feeselector.component.token.FeeTokenSelectorComponent
import com.tangem.features.send.v2.feeselector.model.FeeSelectorModel
import com.tangem.features.send.v2.feeselector.route.FeeSelectorRoute
import com.tangem.features.send.v2.feeselector.ui.FeeSelectorModalBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultFeeSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: FeeSelectorParams.FeeSelectorDetailsParams,
    @Assisted private val onDismiss: () -> Unit,
    private val feeExtendedSelectorComponentFactory: FeeExtendedSelectorComponent.Factory,
    private val feeSpeedSelectorComponentFactory: FeeSpeedSelectorComponent.Factory,
    private val feeTokenSelectorComponentFactory: FeeTokenSelectorComponent.Factory,
) : FeeSelectorComponent, AppComponentContext by appComponentContext {

    private val model: FeeSelectorModel = getOrCreateModel(params = params)

    val innerRouter = InnerRouter<FeeSelectorRoute>(
        stackNavigation = model.stackNavigation,
        popCallback = {
            model.stackNavigation.pop { success ->
                if (!success) {
                    dismiss()
                }
            }
        },
    )

    private val contentStack = childStack(
        key = "FeeSelectorDetailsStack",
        source = model.stackNavigation,
        serializer = FeeSelectorRoute.serializer(),
        initialConfiguration = model.getInitialRoute(),
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val contentStack by contentStack.subscribeAsState()

        FeeSelectorModalBottomSheet(
            childStack = contentStack,
            state = state,
            feeSelectorIntents = model,
            feeDisplaySource = params.feeDisplaySource,
            onDismiss = onDismiss,
            onBack = innerRouter::pop,
        )
    }

    private fun screenChild(config: FeeSelectorRoute, componentContext: ComponentContext): ComposableContentComponent {
        val appComponentContext = childByContext(
            componentContext = componentContext,
            router = innerRouter,
        )

        val componentParams = FeeSelectorComponentParams(
            parentParams = params,
            state = model.uiState,
            intents = model,
        )

        return when (config) {
            FeeSelectorRoute.NetworkFee -> feeExtendedSelectorComponentFactory.create(
                appComponentContext = appComponentContext,
                params = componentParams,
            )
            FeeSelectorRoute.ChooseSpeed -> feeSpeedSelectorComponentFactory.create(
                appComponentContext = appComponentContext,
                params = componentParams,
            )
            FeeSelectorRoute.ChooseToken -> feeTokenSelectorComponentFactory.create(
                appComponentContext = appComponentContext,
                params = componentParams,
            )
        }
    }

    @AssistedFactory
    interface Factory : FeeSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: FeeSelectorParams.FeeSelectorDetailsParams,
            onDismiss: () -> Unit,
        ): DefaultFeeSelectorComponent
    }
}