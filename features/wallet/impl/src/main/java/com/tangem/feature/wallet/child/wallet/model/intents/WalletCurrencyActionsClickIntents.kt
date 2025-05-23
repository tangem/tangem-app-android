package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.bottomsheet.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheetConfig
import com.tangem.common.ui.bottomsheet.receive.mapToAddressModels
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.IsCryptoCurrencyCoinCouldHideUseCase
import com.tangem.domain.tokens.RemoveCurrencyUseCase
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.Companion.AVAILABLE
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.Companion.toReasonAnalyticsText
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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

    fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus, event: AnalyticsEvent? = null)

    fun onStakeClick(cryptoCurrencyStatus: CryptoCurrencyStatus, yield: Yield?)

    fun onCopyAddressLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus): TextReference?

    fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onExploreClick()

    fun onAnalyticsClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onMultiWalletBuyClick(userWalletId: UserWalletId, screenType: String)

    fun onMultiWalletSellClick(userWalletId: UserWalletId)

    fun onMultiWalletSwapClick(userWalletId: UserWalletId)
}

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class WalletCurrencyActionsClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val isCryptoCurrencyCoinCouldHide: IsCryptoCurrencyCoinCouldHideUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: ReduxStateHolder,
    private val vibratorHapticManager: VibratorHapticManager,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
    private val appRouter: AppRouter,
    private val rampStateManager: RampStateManager,
    private val onrampFeatureToggles: OnrampFeatureToggles,
) : BaseWalletClickIntents(), WalletCurrencyActionsClickIntents {

    override fun onSendClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonSend(
                token = cryptoCurrencyStatus.currency.symbol,
                status = unavailabilityReason.toReasonAnalyticsText(),
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWallet.walletId))
        val route = AppRoute.Send(
            currency = cryptoCurrencyStatus.currency,
            userWalletId = userWallet.walletId,
        )

        appRouter.push(route)
    }

    override fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus, event: AnalyticsEvent?) {
        val userWalletId = stateHolder.getSelectedWalletId()

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonReceive(
                token = cryptoCurrencyStatus.currency.symbol,
                status = AVAILABLE,
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        analyticsEventHandler.send(
            event = TokenReceiveAnalyticsEvent.ReceiveScreenOpened(cryptoCurrencyStatus.currency.symbol),
        )

        event?.let { analyticsEventHandler.send(it) }

        stateHolder.showBottomSheet(
            createReceiveBottomSheetContent(
                currency = cryptoCurrencyStatus.currency,
                addresses = cryptoCurrencyStatus.value.networkAddress ?: return,
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
        addresses: NetworkAddress,
    ): TangemBottomSheetConfigContent {
        return TokenReceiveBottomSheetConfig(
            asset = TokenReceiveBottomSheetConfig.Asset.Currency(
                name = currency.name,
                symbol = currency.symbol,
            ),
            network = currency.network,
            networkAddress = addresses,
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

        modelScope.launch(dispatchers.main) {
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

        modelScope.launch(dispatchers.main) {
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

        modelScope.launch(dispatchers.io) {
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
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonSell(
                token = cryptoCurrencyStatus.currency.symbol,
                status = unavailabilityReason.toReasonAnalyticsText(),
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        showErrorIfDemoModeOrElse {
            modelScope.launch(dispatchers.main) {
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
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonBuy(
                token = cryptoCurrencyStatus.currency.symbol,
                status = unavailabilityReason.toReasonAnalyticsText(),
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        if (onrampFeatureToggles.isFeatureEnabled) {
            appRouter.push(
                AppRoute.Onramp(
                    userWalletId = userWallet.walletId,
                    currency = cryptoCurrencyStatus.currency,
                    source = OnrampSource.TOKEN_LONG_TAP,
                ),
            )
        } else {
            showErrorIfDemoModeOrElse {
                modelScope.launch(dispatchers.main) {
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
    }

    override fun onSwapClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        userWalletId: UserWalletId,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonExchange(
                token = cryptoCurrencyStatus.currency.symbol,
                status = unavailabilityReason.toReasonAnalyticsText(),
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        appRouter.push(
            AppRoute.Swap(
                currencyFrom = cryptoCurrencyStatus.currency,
                userWalletId = userWalletId,
                screenSource = AnalyticsParam.ScreensSources.LongTap.value,
            ),
        )
    }

    override fun onExploreClick() {
        showErrorIfDemoModeOrElse(action = ::openExplorer)
    }

    override fun onAnalyticsClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        modelScope.launch {
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

            stateHolder.update(CloseBottomSheetTransformer(userWalletId = stateHolder.getSelectedWalletId()))
        }
    }

    override fun onStakeClick(cryptoCurrencyStatus: CryptoCurrencyStatus, yield: Yield?) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return
        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWallet.walletId))

        modelScope.launch {
            val userWalletId = stateHolder.getSelectedWalletId()
            val cryptoCurrency = cryptoCurrencyStatus.currency

            appRouter.push(
                AppRoute.Staking(
                    userWalletId = userWalletId,
                    cryptoCurrencyId = cryptoCurrency.id,
                    yieldId = yield?.id ?: return@launch,
                ),
            )
        }
    }

    override fun onMultiWalletSellClick(userWalletId: UserWalletId) {
        onMultiWalletActionClick(
            statusFlow = rampStateManager.getSellInitializationStatus(),
            route = AppRoute.SellCrypto(userWalletId = userWalletId),
            eventCreator = MainScreenAnalyticsEvent::ButtonSell,
        )
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

        modelScope.launch {
            val swapRoute = getSwapRoute(
                AppRoute.SwapCrypto(userWalletId = userWalletId),
            )
            onMultiWalletActionClick(
                statusFlow = rampStateManager.getExpressInitializationStatus(userWalletId),
                route = swapRoute,
                eventCreator = MainScreenAnalyticsEvent::ButtonSwap,
            )
        }
    }

    override fun onMultiWalletBuyClick(userWalletId: UserWalletId, screenType: String) {
        onMultiWalletActionClick(
            statusFlow = if (onrampFeatureToggles.isFeatureEnabled) {
                rampStateManager.getExpressInitializationStatus(userWalletId)
            } else {
                rampStateManager.getBuyInitializationStatus()
            },
            route = AppRoute.BuyCrypto(userWalletId = userWalletId),
            eventCreator = { MainScreenAnalyticsEvent.ButtonBuy(status = it, screenType = screenType) },
        )
    }

    private fun openExplorer() {
        val userWalletId = stateHolder.getSelectedWalletId()

        modelScope.launch(dispatchers.main) {
            val currencyStatus = getSingleCryptoCurrencyStatusUseCase.unwrap(userWalletId) ?: return@launch

            when (val addresses = currencyStatus.value.networkAddress) {
                is NetworkAddress.Selectable -> {
                    showChooseAddressBottomSheet(userWalletId, addresses, currencyStatus.currency)
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
        addresses: NetworkAddress,
        currency: CryptoCurrency,
    ) {
        stateHolder.showBottomSheet(
            ChooseAddressBottomSheetConfig(
                asset = TokenReceiveBottomSheetConfig.Asset.Currency(
                    name = currency.name,
                    symbol = currency.symbol,
                ),
                network = currency.network,
                networkAddress = addresses,
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
        modelScope.launch(dispatchers.main) {
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

        modelScope.launch(dispatchers.main) {
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
        modelScope.launch {
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

    private suspend fun getSwapRoute(targetRoute: AppRoute): AppRoute {
        val maybeSwapStories = getStoryContentUseCase.invokeSync(StoryContentIds.STORY_FIRST_TIME_SWAP.id)
        val showSwapStories = maybeSwapStories.getOrNull() != null

        return if (showSwapStories) {
            AppRoute.Stories(
                storyId = StoryContentIds.STORY_FIRST_TIME_SWAP.id,
                nextScreen = targetRoute,
                screenSource = AnalyticsParam.ScreensSources.Main.value,
            )
        } else {
            targetRoute
        }
    }
}
