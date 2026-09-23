package com.tangem.features.collectibles.impl.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.usecase.CloseCollectiblesVaultUseCase
import com.tangem.domain.collectibles.usecase.GetCollectiblesVaultTopUpAddressUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.main.CollectiblesMainBottomSheetConfig
import com.tangem.features.collectibles.impl.main.CollectiblesMainComponent
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesActionsConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesBalanceConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesMenuItem
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesMenuConverter
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Model of the screen's own chrome — balance, actions, the collection widget and the menu. The packs catalogue
 * itself lives in the embedded discovery component, and the collection is a stand-in until its service exists.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class CollectiblesMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val closeCollectiblesVaultUseCase: CloseCollectiblesVaultUseCase,
    private val getCollectiblesVaultTopUpAddressUseCase: GetCollectiblesVaultTopUpAddressUseCase,
    private val clipboardManager: ClipboardManager,
    private val collectiblesAccountStatusSupplier: CollectiblesAccountStatusSupplier,
    private val collectiblesAccountStatusFetcher: CollectiblesAccountStatusFetcher,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
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

    val bottomSheetNavigation = SlotNavigation<CollectiblesMainBottomSheetConfig>()

    val uiState: StateFlow<CollectiblesMainUM>
        field = MutableStateFlow(buildInitialState())

    private var vaultCurrency: CryptoCurrency.Token? = null

    init {
        observeBalance()
    }

    private fun observeBalance() {
        combine(
            flow = collectiblesAccountStatusSupplier(params.userWalletId),
            flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        ) { status, appCurrency ->
            vaultCurrency = status.balance?.usdcToken

            val balance = balanceConverter.convert(
                CollectiblesBalanceConverter.Input(status = status, appCurrency = appCurrency),
            )

            balance to actionsConverter.convert(status)
        }
            .onEach { (balance, actions) ->
                uiState.update { state -> state.copy(balance = balance, actions = actions) }
            }
            .launchIn(modelScope)
    }

    private fun buildInitialState(): CollectiblesMainUM {
        return CollectiblesMainUM(
            title = TextReference.Res(R.string.collectibles_main_title),
            balance = balanceConverter.convert(
                CollectiblesBalanceConverter.Input(
                    status = CollectiblesAccountStatusValue.Loading,
                    appCurrency = AppCurrency.Default,
                ),
            ),
            actions = actionsConverter.convert(CollectiblesAccountStatusValue.Loading),
            collection = CollectiblesMainUM.CollectionUM(
                subtitle = TextReference.Res(R.string.collectibles_account_collection_row_subtitle),
                // TODO for 0 card collectibles_account_collection_empty
                title = TextReference.PluralRes(
                    R.plurals.collectibles_account_collection_cards_count,
                    STUB_CARDS_COUNT,
                    wrappedList(STUB_CARDS_COUNT),
                ),
                onClick = ::onCollectionClick,
            ),
            menu = menuConverter.convert(Unit),
            isBalanceRefreshing = false,
            onBackClick = ::onBackClick,
        )
    }

    fun onRefresh() {
        modelScope.launch {
            uiState.update { state -> state.copy(isBalanceRefreshing = true) }

            collectiblesAccountStatusFetcher(CollectiblesAccountStatusFetcher.Params(params.userWalletId))
                .onLeft { error -> TangemLogger.e("Unable to refresh the collectibles account balance", error) }

            uiState.update { state -> state.copy(isBalanceRefreshing = false) }
        }
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
            CollectiblesMenuItem.COPY_TOP_UP_ADDRESS -> onCopyTopUpAddressClick()
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

    private fun onAddFundsClick() {
        bottomSheetNavigation.activate(CollectiblesMainBottomSheetConfig.TopUp)
    }

    fun onDismissAddFunds() {
        bottomSheetNavigation.dismiss()
    }

    fun onFromTangemWalletClick() {
        val currency = vaultCurrency ?: return sendUnknownErrorMessage()

        bottomSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                userWalletId = params.userWalletId,
                fromCryptoCurrency = currency,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                // TODO AnalyticsParam.ScreensSources.Collectibles.value ?
                screenSource = "Collectibles",
                accountFlow = AppRoute.Swap.AccountFlow.CollectiblesTopUp,
            ),
        )
    }

    private fun sendUnknownErrorMessage() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
    }

    @Suppress("EmptyFunctionBlock")
    fun onFromAnotherWalletClick() {
        // TODO([REDACTED_TASK_KEY]): which address to show is unresolved. Exchanges require a withdrawal address owned by the
        // System Program with no data, which neither the vault's ATA nor its PDA is, and a sender that derives an
        // ATA from the ATA creates a nested account whose funds cannot be moved.
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

    private fun onCopyTopUpAddressClick() {
        onMenuDismiss()

        modelScope.launch {
            val userWallet = userWalletsListRepository.getSyncOrNull(params.userWalletId) ?: return@launch

            getCollectiblesVaultTopUpAddressUseCase(userWallet).fold(
                ifLeft = { error ->
                    TangemLogger.e("Unable to read the collectibles top-up address: $error")
                    messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
                },
                ifRight = { address ->
                    clipboardManager.setText(text = address, isSensitive = false)
                    messageSender.send(
                        SnackbarMessage(message = resourceReference(R.string.address_book_address_copied)),
                    )
                },
            )
        }
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

    private companion object {

        const val STUB_CARDS_COUNT = 25
    }
}