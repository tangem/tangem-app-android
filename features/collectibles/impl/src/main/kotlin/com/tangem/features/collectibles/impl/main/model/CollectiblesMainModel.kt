package com.tangem.features.collectibles.impl.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.collectibles.usecase.CloseCollectiblesVaultUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.main.CollectiblesMainComponent
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesActionsConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesBalanceConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesMenuItem
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesMenuConverter
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Model of the screen's own chrome — balance, actions, the collection widget and the menu. The packs catalogue
 * itself lives in the embedded discovery component. The balance and the collection are stand-ins (see
 * [CollectiblesMainStubData]) until their services exist.
 */
@ModelScoped
internal class CollectiblesMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val closeCollectiblesVaultUseCase: CloseCollectiblesVaultUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<CollectiblesMainComponent.Params>()

    private val balanceConverter = CollectiblesBalanceConverter()
    private val actionsConverter = CollectiblesActionsConverter(
        onAddFundsClick = ::onAddFundsClick,
        onWithdrawalClick = ::onWithdrawalClick,
    )
    private val menuConverter = CollectiblesMenuConverter(
        onMenuClick = ::onMenuClick,
        onDismiss = ::onMenuDismiss,
        onItemClick = ::onMenuItemClick,
    )

    val uiState: StateFlow<CollectiblesMainUM>
        field = MutableStateFlow(buildInitialState())

    private fun buildInitialState(): CollectiblesMainUM {
        return CollectiblesMainUM(
            title = stringReference("Packs account"),
            balance = balanceConverter.convert(CollectiblesMainStubData.BALANCE),
            actions = actionsConverter.convert(Unit),
            collection = CollectiblesMainUM.CollectionUM(
                subtitle = stringReference("My collection"),
                title = stringReference("25 cards"),
                onClick = ::onCollectionClick,
            ),
            menu = menuConverter.convert(Unit),
            onBackClick = ::onBackClick,
        )
    }

    private fun onMenuClick() {
        uiState.update { state -> state.copy(menu = state.menu.copy(isShown = true)) }
    }

    private fun onMenuDismiss() {
        uiState.update { state -> state.copy(menu = state.menu.copy(isShown = false)) }
    }

    private fun onMenuItemClick(item: CollectiblesMenuItem) {
        when (item) {
            CollectiblesMenuItem.ACTIVITY_LOG -> onActivityLogClick()
            CollectiblesMenuItem.DELIVERIES -> onDeliveriesClick()
            CollectiblesMenuItem.HOW_IT_WORKS -> onHowItWorksClick()
            CollectiblesMenuItem.GET_ASSISTANCE -> onGetAssistanceClick()
            CollectiblesMenuItem.CLOSE_ACCOUNT -> onCloseAccountClick()
        }
    }

    private fun onHowItWorksClick() {
        onMenuDismiss()
        router.push(CollectiblesRoute.Stories)
    }

    private fun onBackClick() {
        router.pop()
    }

    @Suppress("EmptyFunctionBlock")
    private fun onAddFundsClick() {
        // TODO([REDACTED_TASK_KEY]): top-up flow is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onWithdrawalClick() {
        // TODO([REDACTED_TASK_KEY]): withdrawal flow is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onCollectionClick() {
        // TODO([REDACTED_TASK_KEY]): the collection screen is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onActivityLogClick() {
        // TODO([REDACTED_TASK_KEY]): the activity log screen is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onDeliveriesClick() {
        // TODO([REDACTED_TASK_KEY]): the deliveries screen is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onGetAssistanceClick() {
        // TODO([REDACTED_TASK_KEY]): support entry point is not wired yet.
    }

    private fun onCloseAccountClick() {
        onMenuDismiss()

        modelScope.launch {
            val userWallet = userWalletsListRepository.getSyncOrNull(params.userWalletId) ?: return@launch

            closeCollectiblesVaultUseCase(userWallet).fold(
                ifLeft = ::onCloseFailed,
                ifRight = { router.pop() },
            )
        }
    }

    private fun onCloseFailed(failure: CollectiblesAccountError) {
        // A card refusal is already on the NFC sheet, and a cancellation is the user's own choice: neither needs one
        if (failure is CollectiblesAccountError.CardError || failure is CollectiblesAccountError.Cancelled) return

        messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
    }
}