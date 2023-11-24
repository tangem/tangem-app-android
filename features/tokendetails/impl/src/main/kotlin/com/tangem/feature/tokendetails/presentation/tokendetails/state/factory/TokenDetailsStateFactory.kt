package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsDialogConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadedTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter.TokenDetailsLoadingTxHistoryModel
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange.ExchangeStatusBottomSheetConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class TokenDetailsStateFactory(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: TokenDetailsClickIntents,
    symbol: String,
    decimals: Int,
) {

    private val skeletonStateConverter by lazy {
        TokenDetailsSkeletonStateConverter(clickIntents = clickIntents)
    }

    private val notificationConverter by lazy {
        TokenDetailsNotificationConverter(clickIntents = clickIntents)
    }

    private val tokenDetailsLoadedBalanceConverter by lazy {
        TokenDetailsLoadedBalanceConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
            symbol = symbol,
            decimals = decimals,
            clickIntents = clickIntents,
        )
    }

    private val tokenDetailsButtonsConverter by lazy {
        TokenDetailsActionButtonsConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
        )
    }

    private val loadingTransactionsStateConverter by lazy {
        TokenDetailsLoadingTxHistoryConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
        )
    }

    private val loadedTxHistoryConverter by lazy {
        TokenDetailsLoadedTxHistoryConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
            symbol = symbol,
            decimals = decimals,
        )
    }

    private val refreshStateConverter by lazy {
        TokenDetailsRefreshStateConverter(
            currentStateProvider = currentStateProvider,
        )
    }

    fun getInitialState(screenArgument: CryptoCurrency): TokenDetailsState {
        return skeletonStateConverter.convert(value = screenArgument)
    }

    fun getCurrencyLoadedBalanceState(
        cryptoCurrencyEither: Either<CurrencyStatusError, CryptoCurrencyStatus>,
    ): TokenDetailsState {
        return tokenDetailsLoadedBalanceConverter.convert(cryptoCurrencyEither)
    }

    fun getManageButtonsState(actions: List<TokenActionsState.ActionState>): TokenDetailsState {
        return tokenDetailsButtonsConverter.convert(actions)
    }

    fun getLoadingTxHistoryState(): TokenDetailsState {
        return currentStateProvider().copy(
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
        )
    }

    fun getLoadingTxHistoryState(
        itemsCountEither: Either<TxHistoryStateError, Int>,
        pendingTransactions: List<TransactionState>,
    ): TokenDetailsState {
        return loadingTransactionsStateConverter.convert(
            value = TokenDetailsLoadingTxHistoryModel(
                historyLoadingState = itemsCountEither,
                pendingTransactions = pendingTransactions,
            ),
        )
    }

    fun getLoadedTxHistoryState(
        txHistoryEither: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            txHistoryState = loadedTxHistoryConverter.convert(txHistoryEither),
        )
    }

    fun getStateWithClosedDialog(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(dialogConfig = state.dialogConfig?.copy(isShow = false))
    }

    fun getStateWithConfirmHideTokenDialog(currency: CryptoCurrency): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.ConfirmHideConfig(
                    currencySymbol = currency.symbol,
                    onConfirmClick = clickIntents::onHideConfirmed,
                    onCancelClick = clickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithLinkedTokensDialog(currency: CryptoCurrency): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.HasLinkedTokensConfig(
                    currencySymbol = currency.symbol,
                    networkName = currency.network.name,
                    onConfirmClick = clickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getRefreshingState(): TokenDetailsState {
        return refreshStateConverter.convert(true)
    }

    fun getRefreshedState(): TokenDetailsState {
        return refreshStateConverter.convert(false)
    }

    fun getStateWithReceiveBottomSheet(
        currency: CryptoCurrency,
        networkAddress: NetworkAddress,
        sendCopyAnalyticsEvent: () -> Unit,
        sendShareAnalyticsEvent: () -> Unit,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissBottomSheet,
                content = TokenReceiveBottomSheetConfig(
                    name = currency.name,
                    symbol = currency.symbol,
                    network = currency.network.name,
                    addresses = networkAddress.availableAddresses.mapToAddressModels(currency).toImmutableList(),
                    onCopyClick = sendCopyAnalyticsEvent,
                    onShareClick = sendShareAnalyticsEvent,
                ),
            ),
        )
    }

    fun getStateWithChooseAddressBottomSheet(
        currency: CryptoCurrency,
        networkAddress: NetworkAddress,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissBottomSheet,
                content = ChooseAddressBottomSheetConfig(
                    addressModels = networkAddress.availableAddresses.mapToAddressModels(currency).toImmutableList(),
                    onClick = clickIntents::onAddressTypeSelected,
                ),
            ),
        )
    }

    fun getStateWithClosedBottomSheet(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            bottomSheetConfig = state.bottomSheetConfig?.copy(isShow = false),
        )
    }

    fun getStateWithUpdatedHidden(isBalanceHidden: Boolean): TokenDetailsState {
        val currentState = currentStateProvider()

        return currentState.copy(isBalanceHidden = isBalanceHidden)
    }

    fun getStateWithNotifications(warnings: Set<CryptoCurrencyWarning>): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.convert(warnings))
    }

    fun getStateWithRemovedRentNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeRentInfo(state))
    }

    fun getStateWithExchangeStatusBottomSheet(swapTxState: SwapTransactionsState): TokenDetailsState {
        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissBottomSheet,
                content = ExchangeStatusBottomSheetConfig(
                    value = swapTxState,
                ),
            ),
        )
    }

    fun getStateAndTriggerEvent(
        state: TokenDetailsState,
        errorMessage: TextReference,
        setUiState: (TokenDetailsState) -> Unit,
    ): TokenDetailsState {
        return state.copy(
            event = triggeredEvent(
                data = errorMessage,
                onConsume = {
                    val currentState = currentStateProvider()
                    setUiState(currentState.copy(event = consumedEvent()))
                },
            ),
        )
    }
}