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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.NetworkHasDerivationUseCase
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsAppBarMenuConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsDialogConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadedTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter.TokenDetailsLoadingTxHistoryModel
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange.ExchangeStatusBottomSheetConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
internal class TokenDetailsStateFactory(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val stakingEntryInfoProvider: Provider<StakingEntryInfo?>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val clickIntents: TokenDetailsClickIntents,
    private val featureToggles: TokenDetailsFeatureToggles,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val userWalletId: UserWalletId,
    stakingFeatureToggles: StakingFeatureToggles,
    symbol: String,
    decimals: Int,
) {

    private val skeletonStateConverter by lazy {
        TokenDetailsSkeletonStateConverter(
            clickIntents = clickIntents,
            featureToggles = featureToggles,
            networkHasDerivationUseCase = networkHasDerivationUseCase,
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
            stakingEntryInfoProvider = stakingEntryInfoProvider,
            symbol = symbol,
            decimals = decimals,
            clickIntents = clickIntents,
            stakingFeatureToggles = stakingFeatureToggles,
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

    private val stakingStateConverter by lazy {
        TokenStakingStateConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
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

    fun getStateWithActionButtonErrorDialog(unavailabilityReason: ScenarioUnavailabilityReason): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.DisabledButtonReasonDialogConfig(
                    text = getUnavailabilityReasonText(unavailabilityReason),
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

    fun getStateWithUpdatedStakingAvailability(stakingAvailability: StakingAvailability): TokenDetailsState {
        return currentStateProvider().copy(
            isStakingBlockShown = stakingAvailability != StakingAvailability.Unavailable,
        )
    }

    fun getStateWithStaking(stakingEither: Either<StakingError, StakingEntryInfo>): TokenDetailsState {
        return currentStateProvider().copy(
            stakingBlocksState = stakingStateConverter.convert(stakingEither),
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

    fun getStateWithRemovedHederaAssociateNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeHederaAssociateWarning(state))
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

    fun updateStateWithExchangeStatusBottomSheet(swapTxState: SwapTransactionsState): TangemBottomSheetConfig? {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetConfig
        val currentConfig = bottomSheetConfig?.content as? ExchangeStatusBottomSheetConfig ?: return bottomSheetConfig
        return bottomSheetConfig.copy(
            content = if (currentConfig.value != swapTxState) {
                ExchangeStatusBottomSheetConfig(swapTxState)
            } else {
                currentConfig
            },
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

    fun getStateWithUpdatedMenu(
        cardTypesResolver: CardTypesResolver,
        hasDerivations: Boolean,
        isBitcoin: Boolean,
    ): TokenDetailsState {
        return with(currentStateProvider()) {
            copy(
                topAppBarConfig = topAppBarConfig.copy(
                    tokenDetailsAppBarMenuConfig = topAppBarConfig.tokenDetailsAppBarMenuConfig
                        ?.updateMenu(cardTypesResolver, hasDerivations, isBitcoin),
                ),
            )
        }
    }

    fun getStateWithUpdatedBalanceSegmentedButtonConfig(
        buttonConfig: TokenBalanceSegmentedButtonConfig,
    ): TokenDetailsState {
        return balanceSelectStateConverter.convert(buttonConfig)
    }

    private fun TokenDetailsAppBarMenuConfig.updateMenu(
        cardTypesResolver: CardTypesResolver,
        hasDerivations: Boolean,
        isBitcoin: Boolean,
    ): TokenDetailsAppBarMenuConfig? {
        if (cardTypesResolver.isSingleWalletWithToken()) return null
        val showGenerateExtendedKey = featureToggles.isGenerateXPubEnabled() && isBitcoin
        return copy(
            items = buildList {
                if (showGenerateExtendedKey && hasDerivations) {
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

    private fun getUnavailabilityReasonText(unavailabilityReason: ScenarioUnavailabilityReason): TextReference {
        return when (unavailabilityReason) {
            is ScenarioUnavailabilityReason.StakingUnavailable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_staking_unavailable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            is ScenarioUnavailabilityReason.PendingTransaction -> {
                when (unavailabilityReason.withdrawalScenario) {
                    ScenarioUnavailabilityReason.WithdrawalScenario.SEND -> resourceReference(
                        id = R.string.token_button_unavailability_reason_pending_transaction_send,
                        formatArgs = wrappedList(unavailabilityReason.networkName),
                    )
                    ScenarioUnavailabilityReason.WithdrawalScenario.SELL -> resourceReference(
                        id = R.string.token_button_unavailability_reason_pending_transaction_sell,
                        formatArgs = wrappedList(unavailabilityReason.networkName),
                    )
                }
            }
            is ScenarioUnavailabilityReason.EmptyBalance -> {
                when (unavailabilityReason.withdrawalScenario) {
                    ScenarioUnavailabilityReason.WithdrawalScenario.SEND -> resourceReference(
                        id = R.string.token_button_unavailability_reason_empty_balance_send,
                    )
                    ScenarioUnavailabilityReason.WithdrawalScenario.SELL -> resourceReference(
                        id = R.string.token_button_unavailability_reason_empty_balance_sell,
                    )
                }
            }
            is ScenarioUnavailabilityReason.BuyUnavailable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_buy_unavailable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            is ScenarioUnavailabilityReason.NotExchangeable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_not_exchangeable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            is ScenarioUnavailabilityReason.NotSupportedBySellService -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_sell_unavailable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            ScenarioUnavailabilityReason.Unreachable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_generic_description,
                )
            }
            ScenarioUnavailabilityReason.UnassociatedAsset -> resourceReference(
                id = R.string.warning_receive_blocked_hedera_token_association_required_message,
            )
            ScenarioUnavailabilityReason.None -> {
                throw IllegalArgumentException("The unavailability reason must be other than None")
            }
        }
    }
}
