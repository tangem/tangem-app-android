package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletNamesUseCase
import com.tangem.domain.wallets.usecase.RenameWalletUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface WalletCardClickIntents {

    fun onRenameBeforeConfirmationClick(userWalletId: UserWalletId)

    fun onRenameAfterConfirmationClick(userWalletId: UserWalletId, name: String)

    fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId)

    fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId)
}

// TODO: Refactor
@Suppress("LongParameterList")
internal class WalletCardClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val renameWalletUseCase: RenameWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val getWalletNamesUseCase: GetWalletNamesUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(), WalletCardClickIntents {

    override fun onRenameBeforeConfirmationClick(userWalletId: UserWalletId) {
        analyticsEventHandler.send(MainScreen.EditWalletTapped)

        viewModelScope.launch(dispatchers.main) {
            val walletNames = getWalletNamesUseCase()
            walletEventSender.send(
                event = WalletEvent.ShowAlert(
                    state = WalletAlertState.RenameWalletAlert(
                        text = stateHolder.getSelectedWallet().walletCardState.title,
                        onConfirmClick = { onRenameAfterConfirmationClick(userWalletId, it) },
                        errorTextProvider = { if (walletNames.contains(it)) "errorText" else null }
                    ),
                ),
            )
        }
    }

    override fun onRenameAfterConfirmationClick(userWalletId: UserWalletId, name: String) {
        viewModelScope.launch(dispatchers.main) {
            val result = renameWalletUseCase(userWalletId = userWalletId, name)
            result.fold(
                ifLeft = {
                    if (it is UpdateWalletError.NameAlreadyExists) {
                        walletEventSender.send(event = WalletEvent.HideAlert)
                    }
                },
                ifRight = {},
            )
        }
    }

    override fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId) {
        analyticsEventHandler.send(MainScreen.DeleteWalletTapped)

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
                .onLeft { Timber.e(it.toString()) }
        }
    }
}
