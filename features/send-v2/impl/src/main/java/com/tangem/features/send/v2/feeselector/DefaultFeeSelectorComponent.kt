package com.tangem.features.send.v2.feeselector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.speed.FeeSpeedSelectorComponent
import com.tangem.features.send.v2.feeselector.model.FeeSelectorModel
import com.tangem.features.send.v2.feeselector.route.FeeSelectorRoute
import com.tangem.features.send.v2.feeselector.ui.FeeSelectorModalBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("PropertyUsedBeforeDeclaration")
internal class DefaultFeeSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: FeeSelectorParams.FeeSelectorDetailsParams,
    @Assisted private val onDismiss: () -> Unit,
    private val feeSpeedSelectorComponentFactory: FeeSpeedSelectorComponent.Factory,
) : FeeSelectorComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = instanceKeeper.getOrCreateSimple { StackNavigation<FeeSelectorRoute>() }
    private val innerRouter = InnerRouter<FeeSelectorRoute>(
        stackNavigation = stackNavigation,
        popCallback = { model.onChildBack() },
    )
    private val model: FeeSelectorModel = getOrCreateModel(params = params, router = innerRouter)

    private val contentStack = childStack(
        key = "FeeSelectorDetailsStack",
        source = stackNavigation,
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
            onDismiss = ::dismiss,
            state = state,
            feeSelectorIntents = model.intents,
            feeDisplaySource = params.feeDisplaySource,
        )
    }

    private fun screenChild(config: FeeSelectorRoute, componentContext: ComponentContext): ComposableContentComponent {
        val appComponentContext = childByContext(
            componentContext = componentContext,
            router = innerRouter,
        )

        val componentParams = FeeSelectorComponentParams(
            state = model.uiState,
            intents = model.intents,
        )

        return when (config) {
            FeeSelectorRoute.ChooseSpeed -> feeSpeedSelectorComponentFactory.create(
                appComponentContext = appComponentContext,
                params = componentParams,
            )
            FeeSelectorRoute.ChooseToken -> TODO()
            FeeSelectorRoute.NetworkFee -> TODO()
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