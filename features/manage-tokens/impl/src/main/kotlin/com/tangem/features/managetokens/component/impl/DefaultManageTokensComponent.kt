package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.AddCustomTokenMode
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.managetokens.ManageTokensBottomSheetConfig
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.model.ManageTokensModel
import com.tangem.features.managetokens.ui.ManageTokensScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultManageTokensComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: ManageTokensComponent.Params,
    private val addCustomTokenComponentFactory: AddCustomTokenComponent.Factory,
) : ManageTokensComponent, AppComponentContext by context {

    private val model: ManageTokensModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = ManageTokensBottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        ManageTokensScreen(
            modifier = modifier,
            state = state,
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: ManageTokensBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val mode = when (config) {
            is ManageTokensBottomSheetConfig.AddWalletCustomToken -> AddCustomTokenMode.Wallet(config.userWalletId)
            is ManageTokensBottomSheetConfig.AddAccountCustomToken -> AddCustomTokenMode.Account(config.accountId)
        }
        return addCustomTokenComponentFactory.create(
            context = childByContext(componentContext),
            params = AddCustomTokenComponent.Params(
                mode = mode,
                source = params.source,
                onDismiss = model.bottomSheetNavigation::dismiss,
                onCurrencyAdded = { account ->
                    val accountName = account?.accountName as? AccountName.Custom
                    if (accountName != null && (account as? Account.Crypto.Portfolio)?.isMainAccount == false) {
                        messageSender.send(
                            SnackbarMessage(
                                message = resourceReference(
                                    R.string.custom_token_another_account_snackbar_text,
                                    wrappedList(accountName.value),
                                ),
                            ),
                        )
                    }
                    model.reloadList()
                },
            ),
        )
    }

    @AssistedFactory
    interface Factory : ManageTokensComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ManageTokensComponent.Params,
        ): DefaultManageTokensComponent
    }
}