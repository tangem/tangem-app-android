package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.model.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

interface WalletCurrencyActionsClickIntents {

    fun onSendClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSwapClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        userWalletId: UserWalletId,
        unavailabilityReason: ScenarioUnavailabilityReason,
    )

    fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onStakeClick(cryptoCurrencyStatus: CryptoCurrencyStatus, yield: Yield?)

    fun onCopyAddressLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus): TextReference?

    fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onExploreClick()

    fun onAnalyticsClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onMultiWalletBuyClick(userWalletId: UserWalletId)

    fun onMultiWalletSellClick(userWalletId: UserWalletId)

    fun onMultiWalletSwapClick(userWalletId: UserWalletId)
}

@Suppress("LongParameterList", "LargeClass")
@ViewModelScoped
internal class WalletCurrencyActionsClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val isCryptoCurrencyCoinCouldHide: IsCryptoCurrencyCoinCouldHideUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: ReduxStateHolder,
    private val vibratorHapticManager: VibratorHapticManager,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
    private val appRouter: AppRouter,
    private val rampStateManager: RampStateManager,
    private val getUserCountryUseCase: GetUserCountryUseCase,
    private val onrampFeatureToggles: OnrampFeatureToggles,
) : BaseWalletClickIntents(), WalletCurrencyActionsClickIntents {

    override fun onSendClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonSend(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWallet.walletId))
        viewModelScope.launch(dispatchers.main) {
            val maybeFeeCurrencyStatus =
                getFeePaidCryptoCurrencyStatusSyncUseCase(userWallet.walletId, cryptoCurrencyStatus).getOrNull()
            when (val currency = cryptoCurrencyStatus.currency) {
                is CryptoCurrency.Coin -> sendCoin(cryptoCurrencyStatus, userWallet, maybeFeeCurrencyStatus)
                is CryptoCurrency.Token -> sendToken(
                    cryptoCurrency = currency,
                    cryptoCurrencyStatus = cryptoCurrencyStatus.value,
                    feeCurrencyStatus = maybeFeeCurrencyStatus,
                    userWallet = userWallet,
                )
            }
        }
    }

    private fun sendCoin(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        userWallet: UserWallet,
        feeCurrencyStatus: CryptoCurrencyStatus?,
    ) {
        reduxStateHolder.dispatch(
            action = TradeCryptoAction.SendCoin(
                userWallet = userWallet,
                coinStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCurrencyStatus,
            ),
        )
    }

    private fun sendToken(
        cryptoCurrency: CryptoCurrency.Token,
        cryptoCurrencyStatus: CryptoCurrencyStatus.Value,
        feeCurrencyStatus: CryptoCurrencyStatus?,
        userWallet: UserWallet,
    ) {
        viewModelScope.launch(dispatchers.main) {
            getNetworkCoinStatusUseCase(
                userWalletId = userWallet.walletId,
                networkId = cryptoCurrency.network.id,
                derivationPath = cryptoCurrency.network.derivationPath,
                isSingleWalletWithTokens = userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .take(count = 1)
                .collectLatest {
                    it.onRight { coinStatus ->
                        reduxStateHolder.dispatch(
                            action = TradeCryptoAction.SendToken(
                                userWallet = userWallet,
                                tokenCurrency = cryptoCurrency,
                                tokenFiatRate = cryptoCurrencyStatus.fiatRate,
                                coinFiatRate = coinStatus.value.fiatRate,
                                feeCurrencyStatus = feeCurrencyStatus,
                            ),
                        )
                    }
                }
        }
    }

    override fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val userWalletId = stateHolder.getSelectedWalletId()

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonReceive(cryptoCurrencyStatus.currency.symbol),
        )

        analyticsEventHandler.send(
            event = TokenReceiveAnalyticsEvent.ReceiveScreenOpened(cryptoCurrencyStatus.currency.symbol),
        )

        stateHolder.showBottomSheet(
            createReceiveBottomSheetContent(
                currency = cryptoCurrencyStatus.currency,
                addresses = cryptoCurrencyStatus.value.networkAddress?.availableAddresses ?: return,
            ),
            userWalletId,
        )
    }

    override fun onCopyAddressLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus): TextReference? {
        val networkAddress = cryptoCurrencyStatus.value.networkAddress ?: return null
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val addresses = networkAddress.availableAddresses.mapToAddressModels(cryptoCurrency).toImmutableList()
        val defaultAddress = addresses.firstOrNull()?.value ?: return null

        vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
        clipboardManager.setText(text = defaultAddress, isSensitive = true)
        analyticsEventHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(cryptoCurrency.symbol))
        return resourceReference(R.string.wallet_notification_address_copied)
    }

    private fun createReceiveBottomSheetContent(
        currency: CryptoCurrency,
        addresses: Set<NetworkAddress.Address>,
    ): TangemBottomSheetConfigContent {
        return TokenReceiveBottomSheetConfig(
            name = currency.name,
            symbol = currency.symbol,
            network = currency.network.name,
            addresses = addresses.mapToAddressModels(currency).toImmutableList(),
            showMemoDisclaimer = currency.network.transactionExtrasType != Network.TransactionExtrasType.NONE,
            onCopyClick = {
                analyticsEventHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(currency.symbol))
                clipboardManager.setText(text = it, isSensitive = true)
            },
            onShareClick = {
                analyticsEventHandler.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(currency.symbol))
                shareManager.shareText(text = it)
            },
        )
    }

    override fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonCopyAddress(cryptoCurrencyStatus.currency.symbol),
        )

        viewModelScope.launch(dispatchers.main) {
            walletManagersFacade.getDefaultAddress(
                userWalletId = stateHolder.getSelectedWalletId(),
                network = cryptoCurrencyStatus.currency.network,
            )?.let { address ->
                stateHolder.update(CloseBottomSheetTransformer(userWalletId = stateHolder.getSelectedWalletId()))

                clipboardManager.setText(text = address, isSensitive = true)
                walletEventSender.send(event = WalletEvent.CopyAddress)
            }
        }
    }

    override fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrencyStatus.currency.symbol),
        )

        viewModelScope.launch(dispatchers.main) {
            walletEventSender.send(
                event = WalletEvent.ShowAlert(
                    state = getHideTokeAlertConfig(stateHolder.getSelectedWalletId(), cryptoCurrencyStatus),
                ),
            )
        }
    }

    private suspend fun getHideTokeAlertConfig(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): WalletAlertState.DefaultAlert {
        val currency = cryptoCurrencyStatus.currency
        return if (currency is CryptoCurrency.Coin && !isCryptoCurrencyCoinCouldHide(userWalletId, currency)) {
            WalletAlertState.DefaultAlert(
                title = resourceReference(
                    id = R.string.token_details_unable_hide_alert_title,
                    formatArgs = WrappedList(listOf(cryptoCurrencyStatus.currency.name)),
                ),
                message = resourceReference(
                    id = R.string.token_details_unable_hide_alert_message,
                    formatArgs = WrappedList(
                        listOf(
                            cryptoCurrencyStatus.currency.name,
                            cryptoCurrencyStatus.currency.symbol,
                            cryptoCurrencyStatus.currency.network.name,
                        ),
                    ),
                ),
                onConfirmClick = null,
            )
        } else {
            WalletAlertState.DefaultAlert(
                title = resourceReference(
                    id = R.string.token_details_hide_alert_title,
                    formatArgs = WrappedList(listOf(cryptoCurrencyStatus.currency.name)),
                ),
                message = resourceReference(R.string.token_details_hide_alert_message),
                onConfirmClick = { onPerformHideToken(cryptoCurrencyStatus) },
            )
        }
    }

    override fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val userWalletId = stateHolder.getSelectedWalletId()

        viewModelScope.launch(dispatchers.io) {
            removeCurrencyUseCase(userWalletId, cryptoCurrencyStatus.currency)
                .fold(
                    ifLeft = {
                        walletEventSender.send(
                            event = WalletEvent.ShowError(text = resourceReference(R.string.common_error)),
                        )
                    },
                    ifRight = {
                        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWalletId))
                    },
                )
        }
    }

    override fun onSellClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonSell(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        showErrorIfDemoModeOrElse {
            viewModelScope.launch(dispatchers.main) {
                reduxStateHolder.dispatch(
                    action = TradeCryptoAction.Sell(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        appCurrencyCode = getSelectedAppCurrencyUseCase.unwrap().code,
                    ),
                )
            }
        }
    }

    override fun onBuyClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonBuy(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        showErrorIfDemoModeOrElse {
            viewModelScope.launch(dispatchers.main) {
                reduxStateHolder.dispatch(
                    TradeCryptoAction.Buy(
                        userWallet = userWallet,
                        source = OnrampSource.TOKEN_LONG_TAP,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        appCurrencyCode = getSelectedAppCurrencyUseCase.unwrap().code,
                    ),
                )
            }
        }
    }

    override fun onSwapClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        userWalletId: UserWalletId,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonExchange(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        appRouter.push(
            AppRoute.Swap(
                currencyFrom = cryptoCurrencyStatus.currency,
                userWalletId = userWalletId,
            ),
        )
    }

    override fun onExploreClick() {
        showErrorIfDemoModeOrElse(action = ::openExplorer)
    }

    override fun onAnalyticsClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        viewModelScope.launch {
            val rawId = cryptoCurrencyStatus.currency.id.rawCurrencyId ?: return@launch

            val tokenMarketParams = TokenMarketParams(
                id = rawId,
                name = cryptoCurrencyStatus.currency.name,
                symbol = cryptoCurrencyStatus.currency.symbol,
                tokenQuotes = TokenMarketParams.Quotes(
                    currentPrice = cryptoCurrencyStatus.value.fiatRate ?: BigDecimal.ZERO,
                    h24Percent = cryptoCurrencyStatus.value.priceChange,
                    weekPercent = null,
                    monthPercent = null,
                ),
                imageUrl = cryptoCurrencyStatus.currency.iconUrl,
            )
            appRouter.push(
                AppRoute.MarketsTokenDetails(
                    token = tokenMarketParams,
                    appCurrency = getSelectedAppCurrencyUseCase.unwrap(),
                    showPortfolio = false,
                    analyticsParams = AppRoute.MarketsTokenDetails.AnalyticsParams(
                        blockchain = cryptoCurrencyStatus.currency.network.name,
                        source = "Main",
                    ),
                ),
            )
        }
    }

    override fun onStakeClick(cryptoCurrencyStatus: CryptoCurrencyStatus, yield: Yield?) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return
        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWallet.walletId))

        viewModelScope.launch {
            val userWalletId = stateHolder.getSelectedWalletId()
            val cryptoCurrency = cryptoCurrencyStatus.currency

            appRouter.push(
                AppRoute.Staking(
                    userWalletId = userWalletId,
                    cryptoCurrencyId = cryptoCurrency.id,
                    yield = yield ?: return@launch,
                ),
            )
        }
    }

    override fun onMultiWalletSellClick(userWalletId: UserWalletId) {
        viewModelScope.launch {
            val userCountry = getUserCountryUseCase().getOrNull()
            if (userCountry is UserCountry.Russia) {
                handleError(
                    alertState = WalletAlertState.SellingRegionalRestriction,
                    eventCreator = MainScreenAnalyticsEvent::ButtonSell,
                )

                return@launch
            }

            val selectedWallet = stateHolder.getSelectedWallet().walletCardState as? WalletCardState.Content
            if (selectedWallet?.isZeroBalance == true) {
                handleError(
                    alertState = WalletAlertState.InsufficientBalanceForSelling,
                    eventCreator = MainScreenAnalyticsEvent::ButtonSell,
                )

                return@launch
            }

            onMultiWalletActionClick(
                statusFlow = rampStateManager.getSellInitializationStatus(),
                route = AppRoute.SellCrypto(userWalletId = userWalletId),
                eventCreator = MainScreenAnalyticsEvent::ButtonSell,
            )
        }
    }

    override fun onMultiWalletSwapClick(userWalletId: UserWalletId) {
        val selectedWallet = stateHolder.getSelectedWallet() as? WalletState.MultiCurrency.Content ?: return
        val tokenListState = selectedWallet.tokensListState as? WalletTokensListState.ContentState.Content ?: return

        if (tokenListState.items.count { it is TokensListItemUM.Token } < 2) {
            handleError(
                alertState = WalletAlertState.InsufficientTokensCountForSwapping,
                eventCreator = MainScreenAnalyticsEvent::ButtonSwap,
            )

            return
        }

        onMultiWalletActionClick(
            statusFlow = rampStateManager.getExpressInitializationStatus(userWalletId),
            route = AppRoute.SwapCrypto(userWalletId = userWalletId),
            eventCreator = MainScreenAnalyticsEvent::ButtonSwap,
        )
    }

    override fun onMultiWalletBuyClick(userWalletId: UserWalletId) {
        onMultiWalletActionClick(
            statusFlow = if (onrampFeatureToggles.isFeatureEnabled) {
                rampStateManager.getExpressInitializationStatus(userWalletId)
            } else {
                rampStateManager.getBuyInitializationStatus()
            },
            route = AppRoute.BuyCrypto(userWalletId = userWalletId),
            eventCreator = MainScreenAnalyticsEvent::ButtonBuy,
        )
    }

    private fun openExplorer() {
        val userWalletId = stateHolder.getSelectedWalletId()

        viewModelScope.launch(dispatchers.main) {
            val currencyStatus = getPrimaryCurrencyStatusUpdatesUseCase.unwrap(userWalletId) ?: return@launch

            when (val addresses = currencyStatus.value.networkAddress) {
                is NetworkAddress.Selectable -> {
                    showChooseAddressBottomSheet(userWalletId, addresses.availableAddresses, currencyStatus.currency)
                }
                is NetworkAddress.Single -> {
                    router.openUrl(
                        url = getExploreUrlUseCase(
                            userWalletId = userWalletId,
                            currency = currencyStatus.currency,
                            addressType = AddressType.Default,
                        ),
                    )
                }
                null -> Unit
            }
        }
    }

    private fun showChooseAddressBottomSheet(
        userWalletId: UserWalletId,
        addresses: Set<NetworkAddress.Address>,
        currency: CryptoCurrency,
    ) {
        stateHolder.showBottomSheet(
            ChooseAddressBottomSheetConfig(
                addressModels = addresses.mapToAddressModels(currency).toImmutableList(),
                onClick = {
                    onAddressTypeSelected(
                        userWalletId = userWalletId,
                        currency = currency,
                        addressModel = it,
                    )
                },
            ),
            userWalletId,
        )
    }

    private fun onAddressTypeSelected(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        addressModel: AddressModel,
    ) {
        viewModelScope.launch(dispatchers.main) {
            router.openUrl(
                url = getExploreUrlUseCase(
                    userWalletId = userWalletId,
                    currency = currency,
                    addressType = AddressType.valueOf(addressModel.type.name),
                ),
            )

            stateHolder.update(
                CloseBottomSheetTransformer(userWalletId = userWalletId),
            )
        }
    }

    private fun showErrorIfDemoModeOrElse(action: () -> Unit) {
        val cardId = getSelectedWalletSyncUseCase.unwrap()?.cardId ?: return

        if (isDemoCardUseCase(cardId = cardId)) {
            stateHolder.update(CloseBottomSheetTransformer(userWalletId = stateHolder.getSelectedWalletId()))

            walletEventSender.send(
                event = WalletEvent.ShowError(
                    text = resourceReference(id = R.string.alert_demo_feature_disabled),
                ),
            )
        } else {
            action()
        }
    }

    private fun handleUnavailabilityReason(unavailabilityReason: ScenarioUnavailabilityReason): Boolean {
        if (unavailabilityReason == ScenarioUnavailabilityReason.None) return false

        viewModelScope.launch(dispatchers.main) {
            walletEventSender.send(
                event = WalletEvent.ShowAlert(
                    state = WalletAlertState.DefaultAlert(
                        title = null,
                        message = unavailabilityReason.getUnavailabilityReasonText(),
                        onConfirmClick = null,
                    ),
                ),
            )
        }

        return true
    }

    private fun onMultiWalletActionClick(
        statusFlow: Flow<Lce<Throwable, Any>>,
        route: AppRoute,
        eventCreator: (AnalyticsParam.Status) -> MainScreenAnalyticsEvent,
    ) {
        viewModelScope.launch {
            statusFlow.foldStatus(
                onContent = { handleContent(route, eventCreator) },
                onError = { handleError(eventCreator = eventCreator) },
                onLoading = { handleLoading(eventCreator) },
            )
        }
    }

    private suspend fun Flow<Lce<Throwable, Any>>.foldStatus(
        onContent: () -> Unit,
        onError: () -> Unit,
        onLoading: () -> Unit,
    ) {
        val status = firstOrNull() ?: IllegalStateException("Status is null").lceError()

        when (status) {
            is Lce.Content -> onContent()
            is Lce.Error -> onError()
            is Lce.Loading -> onLoading()
        }
    }

    private fun handleContent(route: AppRoute, eventCreator: (AnalyticsParam.Status) -> MainScreenAnalyticsEvent) {
        analyticsEventHandler.send(event = eventCreator(AnalyticsParam.Status.Success))

        appRouter.push(route = route)
    }

    private fun handleError(
        alertState: WalletAlertState = WalletAlertState.UnavailableOperation,
        eventCreator: (AnalyticsParam.Status) -> MainScreenAnalyticsEvent,
    ) {
        analyticsEventHandler.send(event = eventCreator(AnalyticsParam.Status.Error))

        walletEventSender.send(event = WalletEvent.ShowAlert(state = alertState))
    }

    private fun handleLoading(eventCreator: (AnalyticsParam.Status) -> MainScreenAnalyticsEvent) {
        analyticsEventHandler.send(event = eventCreator(AnalyticsParam.Status.Pending))

        walletEventSender.send(
            event = WalletEvent.ShowAlert(state = WalletAlertState.ProvidersStillLoading),
        )
    }
}
