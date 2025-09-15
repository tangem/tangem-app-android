package com.tangem.features.hotwallet.updateaccesscode

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.UpdateAccessCodeComponent
import com.tangem.features.hotwallet.updateaccesscode.routing.UpdateAccessCodeChildFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultUpdateAccessCodeComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: UpdateAccessCodeComponent.Params,
    private val childFactory: UpdateAccessCodeChildFactory,
) : UpdateAccessCodeComponent, AppComponentContext by appComponentContext {

    private val model: UpdateAccessCodeModel = getOrCreateModel(params)

    private val innerStack = childStack(
        key = "hotWalletAccessCodeInnerStack",
        source = model.stackNavigation,
        serializer = null,
        initialConfiguration = model.startRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            childFactory.createChild(
                route = configuration,
                childContext = childByContext(factoryContext),
                model = model,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by innerStack.subscribeAsState()

        BackHandler(onBack = model::onChildBack)

        SetAccessCodeContent(
            onBackClick = model::onChildBack,
            stackState = stackState,
        )
    }

    @AssistedFactory
    interface Factory : UpdateAccessCodeComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: UpdateAccessCodeComponent.Params,
        ): DefaultUpdateAccessCodeComponent
    }
}