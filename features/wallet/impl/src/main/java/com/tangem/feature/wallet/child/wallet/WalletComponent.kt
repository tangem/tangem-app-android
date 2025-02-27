package com.tangem.feature.wallet.child.wallet

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.utils.findActivity
import com.tangem.feature.wallet.child.wallet.model.WalletModel
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.features.markets.entry.MarketsEntryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class WalletComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted navigate: (WalletRoute) -> Unit,
    private val renameWalletComponentFactory: RenameWalletComponent.Factory,
    private val marketsEntryComponentFactory: MarketsEntryComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: WalletModel = getOrCreateModel()

    init {
        lifecycle.subscribe(model.screenLifecycleProvider)
        componentScope.launch { model.innerWalletRouter.navigateToFlow.collect { navigate(it) } }
    }

    private val dialog = childSlot(
        source = model.innerWalletRouter.dialogNavigation,
        serializer = WalletDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { dialogConfig, componentContext ->
            when (dialogConfig) {
                is WalletDialogConfig.RenameWallet -> {
                    renameWalletComponentFactory.create(
                        context = childByContext(componentContext),
                        params = RenameWalletComponent.Params(
                            userWalletId = dialogConfig.userWalletId,
                            currentName = dialogConfig.currentName,
                            onDismiss = model.innerWalletRouter.dialogNavigation::dismiss,
                        ),
                    )
                }
            }
        },
    )

    private val marketsEntryComponent = marketsEntryComponentFactory.create(child("marketsEntryComponent"))

    @Composable
    override fun Content(modifier: Modifier) {
        val activity = LocalContext.current.findActivity()
        BackHandler { activity.finish() }

        val dialog by dialog.subscribeAsState()

        WalletScreen(
            state = model.uiState.collectAsStateWithLifecycle().value,
            marketsEntryComponent = marketsEntryComponent,
        )

        dialog.child?.instance?.Dialog()
    }

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, navigate: (WalletRoute) -> Unit): WalletComponent
    }
}