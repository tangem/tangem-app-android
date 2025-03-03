package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface WalletCardClickIntents {

    fun onRenameBeforeConfirmationClick(userWalletId: UserWalletId)

    fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId)

    fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId)
}

// TODO: Refactor
@Suppress("LongParameterList")
@ModelScoped
internal class WalletCardClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val tokenListStore: MultiWalletTokenListStore,
    private val walletEventSender: WalletEventSender,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val reduxStateHolder: ReduxStateHolder,
    private val appRouter: AppRouter,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(), WalletCardClickIntents {

    override fun onRenameBeforeConfirmationClick(userWalletId: UserWalletId) {
        analyticsEventHandler.send(MainScreen.EditWalletTapped)

        router.dialogNavigation.activate(
            configuration = WalletDialogConfig.RenameWallet(
                userWalletId = userWalletId,
                currentName = stateHolder.getSelectedWallet().walletCardState.title,
            ),
        )
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
        modelScope.launch(dispatchers.main) {
            walletScreenContentLoader.cancel(userWalletId)
            tokenListStore.remove(userWalletId)

            val walletToDelete = getUserWalletUseCase(userWalletId).getOrNull() ?: return@launch
            val hasUserWallets = deleteWalletUseCase(userWalletId).getOrElse {
                Timber.e("Unable to delete user wallet: $it")
                return@launch
            }

            deleteSavedAccessCodesUseCase(cardId = walletToDelete.cardId).onLeft {
                Timber.e("Unable to delete user wallet access code: $it")
            }

            if (hasUserWallets) {
                val selectedWallet = getSelectedWalletSyncUseCase().getOrElse {
                    error("Unable to find selected wallet: $it")
                }

                reduxStateHolder.onUserWalletSelected(selectedWallet)
            } else {
                tokenListStore.clear()
                stateHolder.clear()
                appRouter.replaceAll(AppRoute.Home)
            }
        }
    }
}