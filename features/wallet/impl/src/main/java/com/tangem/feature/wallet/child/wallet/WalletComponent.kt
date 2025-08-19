package com.tangem.feature.wallet.child.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.feature.wallet.child.wallet.model.WalletModel
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.markets.entry.MarketsEntryComponent
import com.tangem.features.pushnotifications.api.PushNotificationsBottomSheetComponent
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class WalletComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted navigate: (WalletRoute) -> Unit,
    private val renameWalletComponentFactory: RenameWalletComponent.Factory,
    private val marketsEntryComponentFactory: MarketsEntryComponent.Factory,
    private val askBiometryComponentFactory: AskBiometryComponent.Factory,
    private val pushNotificationsBottomSheetComponent: PushNotificationsBottomSheetComponent.Factory,
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
                is WalletDialogConfig.AskForBiometry -> {
                    askBiometryComponentFactory.create(
                        context = childByContext(componentContext),
                        params = AskBiometryComponent.Params(
                            bottomSheetVariant = true,
                            modelCallbacks = model.askBiometryModelCallbacks,
                        ),
                    )
                }
                WalletDialogConfig.AskForPushNotifications -> pushNotificationsBottomSheetComponent.create(
                    context = childByContext(componentContext),
                    params = PushNotificationsParams(
                        isBottomSheet = true,
                        modelCallbacks = model.askForPushNotificationsModelCallbacks,
                        source = AppRoute.PushNotification.Source.Main,
                    ),
                )
            }
        },
    )

    private val marketsEntryComponent = marketsEntryComponentFactory.create(child("marketsEntryComponent"))

    @Composable
    override fun Content(modifier: Modifier) {
        val dialog by dialog.subscribeAsState()

        WalletScreen(
            state = model.uiState.collectAsStateWithLifecycle().value,
            marketsEntryComponent = marketsEntryComponent,
        )

        when (val dialog = dialog.child?.instance) {
            is ComposableDialogComponent -> dialog.Dialog()
            is ComposableBottomSheetComponent -> dialog.BottomSheet()
            else -> {}
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, navigate: (WalletRoute) -> Unit): WalletComponent
    }
}