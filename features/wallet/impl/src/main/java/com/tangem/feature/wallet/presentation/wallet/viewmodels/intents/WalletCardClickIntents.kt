package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.navigation.AppScreen
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.UpdateWalletUseCase
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface WalletCardClickIntents {

    fun onRenameClick(userWalletId: UserWalletId, name: String)

    fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId)

    fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId)
}

internal class WalletCardClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(), WalletCardClickIntents {

    override fun onRenameClick(userWalletId: UserWalletId, name: String) {
        viewModelScope.launch(dispatchers.main) {
            updateWalletUseCase(userWalletId = userWalletId, update = { it.copy(name) })
        }
    }

    override fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId) {
        walletEventSender.send(
            event = WalletEvent.ShowAlert(
                state = WalletAlertState.RemoveWalletAlert(
                    onConfirmClick = { onDeleteAfterConfirmationClick(userWalletId) },
                ),
            ),
        )
    }

    override fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId) {
        viewModelScope.launch(dispatchers.main) {
            walletScreenContentLoader.cancel(userWalletId)
            deleteWalletUseCase(userWalletId)
                .onRight { popBackIfAllWalletsIsLocked() }
                .onLeft { Timber.e(it.toString()) }
        }
    }

    private fun popBackIfAllWalletsIsLocked() {
        val wallets = stateHolder.value.wallets.map(WalletState::walletCardState)
        val unlockedWallet = wallets.count { it !is WalletCardState.LockedContent }

        if (unlockedWallet == 1) {
            stateHolder.clear()

            router.popBackStack(
                screen = if (wallets.size > 1) AppScreen.Welcome else AppScreen.Home,
            )
        }
    }
}