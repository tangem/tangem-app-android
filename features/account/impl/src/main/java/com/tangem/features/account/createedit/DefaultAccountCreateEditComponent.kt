package com.tangem.features.account.createedit

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.createedit.ui.AccountCreateEditContent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.builtins.serializer

internal class DefaultAccountCreateEditComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: AccountCreateEditComponent.Params,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : AppComponentContext by appComponentContext, AccountCreateEditComponent {

    private val model: AccountCreateEditModel = getOrCreateModel(params)

    private val walletSelectorSlot = model.walletSelector?.let { selector ->
        childSlot(
            source = selector.picker.navigation,
            serializer = Unit.serializer(),
            key = "accountCreateWalletSelectorSlot",
            handleBackButton = false,
            childFactory = { _, componentContext ->
                portfolioSelectorComponentFactory.create(
                    context = childByContext(componentContext),
                    params = selector.picker.selectorParams,
                )
            },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        AccountCreateEditContent(
            state = state,
            isCreateMode = params is AccountCreateEditComponent.Params.Create,
            modifier = modifier,
        )
        // only while there are unsaved changes: otherwise back is a plain pop, left to the stack's predictive back
        BackHandler(enabled = state.hasUnsavedChanges, onBack = state.onCloseClick)

        walletSelectorSlot?.let { slot ->
            val walletSelector by slot.subscribeAsState()
            walletSelector.child?.instance?.BottomSheet()
        }
    }

    @AssistedFactory
    interface Factory : AccountCreateEditComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AccountCreateEditComponent.Params,
        ): DefaultAccountCreateEditComponent
    }
}