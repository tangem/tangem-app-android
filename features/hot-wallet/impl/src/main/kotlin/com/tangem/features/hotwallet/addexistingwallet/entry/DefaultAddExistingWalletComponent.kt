package com.tangem.features.hotwallet.addexistingwallet.entry

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.AddExistingWalletComponent
import com.tangem.features.hotwallet.addexistingwallet.entry.routing.AddExistingWalletChildFactory
import com.tangem.features.hotwallet.addexistingwallet.entry.ui.AddExistingWalletContent
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.hotwallet.stepper.impl.DefaultHotWalletStepperComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultAddExistingWalletComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Unit,
    private val stepperStateManager: AddExistingWalletStepperStateManager,
    addExistingWalletChildFactory: AddExistingWalletChildFactory,
    stepperComponentFactory: DefaultHotWalletStepperComponent.Factory,
) : AddExistingWalletComponent, AppComponentContext by appComponentContext {

    private val model: AddExistingWalletModel = getOrCreateModel(params)

    private val innerStack = childStack(
        key = "addExistingWalletInnerStack",
        source = model.stackNavigation,
        serializer = null,
        initialConfiguration = model.startRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            addExistingWalletChildFactory.createChild(
                route = configuration,
                childContext = childByContext(factoryContext),
                model = model,
            )
        },
    )

    private val stepperComponent = stepperComponentFactory.create(
        context = this,
        params = HotWalletStepperComponent.Params(
            initState = HotWalletStepperComponent.StepperUM.initialState(),
            callback = model.hotWalletStepperComponentModelCallback,
        ),
    )

    init {
        innerStack.subscribe(
            lifecycle = lifecycle,
            mode = ObserveLifecycleMode.CREATE_DESTROY,
        ) { stack ->
            componentScope.launch {
                model.currentRoute.emit(stack.active.configuration)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by innerStack.subscribeAsState()
        val currentRoute = stackState.active.configuration

        BackHandler(onBack = model::onChildBack)

        val stepperState = stepperStateManager.getStepperState(currentRoute)
        stepperState?.let { stepperComponent.updateState(it) }

        AddExistingWalletContent(
            stackState = stackState,
            stepperComponent = stepperComponent.takeIf { stepperState != null },
        )
    }

    @AssistedFactory
    interface Factory : AddExistingWalletComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAddExistingWalletComponent
    }
}