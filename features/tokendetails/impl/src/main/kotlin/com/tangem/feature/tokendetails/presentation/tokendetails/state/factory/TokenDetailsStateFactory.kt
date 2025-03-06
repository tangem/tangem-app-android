package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.NetworkHasDerivationUseCase
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.staking.GetStakingIntegrationIdUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceSegmentedButtonConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsAppBarMenuConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsDialogConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadedTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter.TokenDetailsLoadingTxHistoryModel
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
internal class TokenDetailsStateFactory(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val clickIntents: TokenDetailsClickIntents,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val userWalletId: UserWalletId,
    getStakingIntegrationIdUseCase: GetStakingIntegrationIdUseCase,
    symbol: String,
    decimals: Int,
) {

    private val skeletonStateConverter by lazy {
        TokenDetailsSkeletonStateConverter(
            clickIntents = clickIntents,
            networkHasDerivationUseCase = networkHasDerivationUseCase,
            getStakingIntegrationIdUseCase = getStakingIntegrationIdUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            userWalletId = userWalletId,
        )
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

    private val balanceSelectStateConverter by lazy {
        TokenDetailsBalanceSelectStateConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
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

    fun getStakingInfoState(
        state: TokenDetailsState,
        stakingEntryInfo: StakingEntryInfo?,
        stakingAvailability: StakingAvailability,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenDetailsState {
        return TokenDetailsStakingInfoConverter(
            currentState = state,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            stakingEntryInfo = stakingEntryInfo,
        ).convert(stakingAvailability)
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
                    currencyTitle = currency.name,
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
                    currencyName = currency.name,
                    currencySymbol = currency.symbol,
                    networkName = currency.network.name,
                    onConfirmClick = clickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithDismissIncompleteTransactionConfirmDialog(): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.RemoveIncompleteTransactionConfirmDialogConfig(
                    onConfirmClick = clickIntents::onConfirmDismissIncompleteTransactionClick,
                    onCancelClick = clickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithActionButtonErrorDialog(unavailabilityReason: ScenarioUnavailabilityReason): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.DisabledButtonReasonDialogConfig(
                    text = unavailabilityReason.getUnavailabilityReasonText(),
                    onConfirmClick = clickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithErrorDialog(text: TextReference): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.ErrorDialogConfig(
                    text = text,
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
        onCopyClick: (String) -> Unit,
        onShareClick: (String) -> Unit,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = clickIntents::onDismissBottomSheet,
                content = TokenReceiveBottomSheetConfig(
                    name = currency.name,
                    symbol = currency.symbol,
                    network = currency.network.name,
                    addresses = networkAddress.availableAddresses.mapToAddressModels(currency).toImmutableList(),
                    showMemoDisclaimer = currency.network.transactionExtrasType != Network.TransactionExtrasType.NONE,
                    onCopyClick = onCopyClick,
                    onShareClick = onShareClick,
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
                isShown = true,
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
            bottomSheetConfig = state.bottomSheetConfig?.copy(isShown = false),
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

    fun getStateWithRemovedHederaAssociateNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeHederaAssociateWarning(state))
    }

    fun getStateWithRemovedKaspaIncompleteTransactionNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            notifications = notificationConverter.removeKaspaIncompleteTransactionWarning(state),
            dialogConfig = state.dialogConfig?.copy(isShow = false),
        )
    }

    fun getStateWithUpdatedMenu(
        cardTypesResolver: CardTypesResolver,
        hasDerivations: Boolean,
        isSupported: Boolean,
    ): TokenDetailsState {
        return with(currentStateProvider()) {
            copy(
                topAppBarConfig = topAppBarConfig.copy(
                    tokenDetailsAppBarMenuConfig = topAppBarConfig.tokenDetailsAppBarMenuConfig
                        ?.updateMenu(cardTypesResolver, hasDerivations, isSupported),
                ),
            )
        }
    }

    fun getStateWithUpdatedBalanceSegmentedButtonConfig(
        buttonConfig: TokenBalanceSegmentedButtonConfig,
    ): TokenDetailsState {
        return balanceSelectStateConverter.convert(buttonConfig)
    }

    fun getStateWithConfirmHideExpressStatus(): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.ConfirmExpressStatusHideDialogConfig(
                    onConfirmClick = {
                        clickIntents.onDisposeExpressStatus()
                        clickIntents.onDismissDialog()
                    },
                    onCancelClick = clickIntents::onDismissDialog,
                ),
            ),
        )
    }

    private fun TokenDetailsAppBarMenuConfig.updateMenu(
        cardTypesResolver: CardTypesResolver,
        hasDerivations: Boolean,
        isSupported: Boolean,
    ): TokenDetailsAppBarMenuConfig? {
        if (cardTypesResolver.isSingleWalletWithToken()) return null
        return copy(
            items = buildList {
                if (isSupported && hasDerivations) {
                    TokenDetailsAppBarMenuConfig.MenuItem(
                        title = resourceReference(R.string.token_details_generate_xpub),
                        textColorProvider = { TangemTheme.colors.text.primary1 },
                        onClick = clickIntents::onGenerateExtendedKey,
                    ).let(::add)
                }
                TokenDetailsAppBarMenuConfig.MenuItem(
                    title = TextReference.Res(id = R.string.token_details_hide_token),
                    textColorProvider = { TangemTheme.colors.text.warning },
                    onClick = clickIntents::onHideClick,
                ).let(::add)
            }.toImmutableList(),
        )
    }
}