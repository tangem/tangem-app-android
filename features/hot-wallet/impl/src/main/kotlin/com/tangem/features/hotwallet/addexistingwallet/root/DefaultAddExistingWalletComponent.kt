package com.tangem.features.hotwallet.addexistingwallet.root

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.AddExistingWalletComponent
import com.tangem.features.hotwallet.addexistingwallet.root.routing.AddExistingWalletChildFactory
import com.tangem.features.hotwallet.addexistingwallet.root.routing.AddExistingWalletRoute
import com.tangem.features.hotwallet.addexistingwallet.root.ui.AddExistingWalletContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddExistingWalletComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Unit,
    addExistingWalletChildFactory: AddExistingWalletChildFactory,
) : AddExistingWalletComponent, AppComponentContext by appComponentContext {

    private val model: AddExistingWalletModel = getOrCreateModel(params)

    private val startRoute = AddExistingWalletRoute.Start

    private val innerStack = childStack(
        key = "addExistingWalletInnerStack",
        source = model.stackNavigation,
        serializer = null,
        initialConfiguration = startRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            addExistingWalletChildFactory.createChild(
                route = configuration,
                childContext = childByContext(factoryContext),
                model = model,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by innerStack.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        AddExistingWalletContent(
            stackState = stackState,
        )
    }

    private fun onChildBack() {
        val isEmptyStack = innerStack.value.backStack.isEmpty()

        if (isEmptyStack) {
            router.pop()
        } else {
            model.stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : AddExistingWalletComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAddExistingWalletComponent
    }
}