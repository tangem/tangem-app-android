package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.impl.R
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
// [REDACTED_TODO_COMMENT]
@Suppress("LongParameterList")
internal class WalletCardClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val renameWalletUseCase: RenameWalletUseCase,
    private val getWalletNamesUseCase: GetWalletNamesUseCase,
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

        viewModelScope.launch(dispatchers.main) {
            val walletNames = getWalletNamesUseCase()
            val currentWalletName = stateHolder.getSelectedWallet().walletCardState.title
            walletEventSender.send(
                event = WalletEvent.ShowAlert(
                    state = WalletAlertState.RenameWalletAlert(
                        text = currentWalletName,
                        onConfirmClick = { onRenameAfterConfirmationClick(userWalletId, it) },
                        errorTextProvider = { enteredName ->
                            if (walletNames.contains(enteredName) && enteredName != currentWalletName) {
                                resourceReference(
                                    R.string.user_wallet_list_rename_popup_error_already_exists,
                                    wrappedList(enteredName),
                                )
                            } else {
                                null
                            }
                        },
                    ),
                ),
            )
        }
    }

    override fun onRenameAfterConfirmationClick(userWalletId: UserWalletId, name: String) {
        viewModelScope.launch(dispatchers.main) {
            renameWalletUseCase(userWalletId = userWalletId, name)
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
                stateHolder.clear()
                appRouter.replaceAll(AppRoute.Home)
            }
        }
    }
}
