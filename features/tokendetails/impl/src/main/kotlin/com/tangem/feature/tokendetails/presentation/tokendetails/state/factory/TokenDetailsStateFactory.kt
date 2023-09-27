package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.blockchain.common.address.Address
import com.tangem.common.Provider
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsDialogConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadedTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.navigation.TokenDetailsArguments
import kotlinx.coroutines.flow.Flow

internal class TokenDetailsStateFactory(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val clickIntents: TokenDetailsClickIntents,
    currencySymbolProvider: Provider<String>,
    currencyDecimalsProvider: Provider<Int>,
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
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            symbol = currencySymbolProvider(),
            decimals = currencyDecimalsProvider(),
        )
    }

    private val tokenDetailsButtonsConverter by lazy {
        TokenDetailsActionButtonsConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
        )
    }

    private val loadingTransactionsStateConverter by lazy {
        TokenDetailsLoadingTxHistoryConverter(currentStateProvider = currentStateProvider, clickIntents = clickIntents)
    }

    private val loadedTxHistoryConverter by lazy {
        TokenDetailsLoadedTxHistoryConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
            symbol = currencySymbolProvider(),
            decimals = currencyDecimalsProvider(),
        )
    }

    private val refreshStateConverter by lazy {
        TokenDetailsRefreshStateConverter(
            currentStateProvider = currentStateProvider,
        )
    }

    fun getInitialState(screenArgument: TokenDetailsArguments): TokenDetailsState {
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

    fun getLoadingTxHistoryState(itemsCountEither: Either<TxHistoryStateError, Int>): TokenDetailsState {
        return loadingTransactionsStateConverter.convert(value = itemsCountEither)
    }

    fun getLoadedTxHistoryState(
        txHistoryEither: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>,
    ): TokenDetailsState {
        return loadedTxHistoryConverter.convert(txHistoryEither)
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
        addresses: List<Address>,
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
                    addresses = addresses.map {
                        AddressModel(
                            value = it.value,
                            type = AddressModel.Type.valueOf(it.type.name),
                        )
                    },
                    onCopyClick = sendCopyAnalyticsEvent,
                    onShareClick = sendShareAnalyticsEvent,
                ),
            ),
        )
    }

    fun getStateWithChooseAddressBottomSheet(
        addresses: List<Address>,
        onAddressTypeClick: (AddressModel) -> Unit,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissBottomSheet,
                content = ChooseAddressBottomSheetConfig(
                    addressModels = addresses.map {
                        AddressModel(
                            value = it.value,
                            type = AddressModel.Type.valueOf(it.type.name),
                        )
                    },
                    onClick = onAddressTypeClick,
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
        val possibleTokenBalanceBlockState = currentState.tokenBalanceBlockState as?
            TokenDetailsBalanceBlockState.Content

        possibleTokenBalanceBlockState?.let {
            return currentState.copy(
                tokenBalanceBlockState = possibleTokenBalanceBlockState.copy(isBalanceHidden = isBalanceHidden),
            )
        } ?: return currentState
    }

    fun getStateWithNotifications(warnings: Set<CryptoCurrencyWarning>): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.convert(warnings))
    }

    fun getStateWithRemovedExistentialNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeExistentialDeposit(state))
    }

    fun getStateWithRemovedRentNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeRentInfo(state))
    }
}
