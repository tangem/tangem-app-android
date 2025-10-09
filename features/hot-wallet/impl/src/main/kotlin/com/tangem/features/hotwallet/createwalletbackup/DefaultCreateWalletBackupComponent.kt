package com.tangem.features.hotwallet.createwalletbackup

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
import com.tangem.features.hotwallet.CreateWalletBackupComponent
import com.tangem.features.hotwallet.createwalletbackup.routing.CreateWalletBackupChildFactory
import com.tangem.features.hotwallet.createwalletbackup.routing.CreateWalletBackupRoute
import com.tangem.features.hotwallet.createwalletbackup.ui.CreateWalletBackupContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultCreateWalletBackupComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: CreateWalletBackupComponent.Params,
    createWalletBackupChildFactory: CreateWalletBackupChildFactory,
) : CreateWalletBackupComponent, AppComponentContext by appComponentContext {

    private val model: CreateWalletBackupModel = getOrCreateModel(params)

    private val innerStack = childStack(
        key = "createWalletBackupInnerStack",
        source = model.stackNavigation,
        serializer = null,
        initialConfiguration = model.startRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            createWalletBackupChildFactory.createChild(
                route = configuration,
                childContext = childByContext(factoryContext),
                model = model,
            )
        },
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

        BackHandler(onBack = model::onBack)

        CreateWalletBackupContent(
            stackState = stackState,
            modifier = modifier,
            showTopBar = currentRoute !is CreateWalletBackupRoute.BackupCompleted,
            onBackClick = model::onBack,
        )
    }

    @AssistedFactory
    interface Factory : CreateWalletBackupComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CreateWalletBackupComponent.Params,
        ): DefaultCreateWalletBackupComponent
    }
}