package com.tangem.feature.wallet.child.wallet.model.intents

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.bottomsheet.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.common.ui.bottomsheet.receive.mapToAddressModels
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.analytics.models.event.OfframpAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.GetOfframpUrlUseCase
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.account.status.usecase.IsCryptoCurrencyCouldHideUseCase
import com.tangem.domain.tokens.NeedShowYieldSupplyDepositedWarningUseCase
import com.tangem.domain.tokens.SaveViewedTokenReceiveWarningUseCase
import com.tangem.domain.tokens.SaveViewedYieldSupplyWarningUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.Companion.AVAILABLE
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.Companion.toReasonAnalyticsText
import com.tangem.domain.tokens.model.details.TokenAction
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

interface WalletCurrencyActionsClickIntents {

    fun onSendClick(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    )

    fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onBuyClick(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    )

    fun onSwapClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        accountId: AccountId,
        unavailabilityReason: ScenarioUnavailabilityReason,
    )

    fun onReceiveClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus, event: AnalyticsEvent? = null)

    fun onStakeClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus, option: StakingOption?)

    fun onCopyAddressLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus): TextReference?

    fun onCopyAddressClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onHideTokensClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onPerformHideToken(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus)

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
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getOfframpUrlUseCase: GetOfframpUrlUseCase,
    private val urlOpener: UrlOpener,
    private val vibratorHapticManager: VibratorHapticManager,
    private val clipboardManager: ClipboardManager,
    private val appRouter: AppRouter,
    private val rampStateManager: RampStateManager,
    private val saveViewedTokenReceiveWarningUseCase: SaveViewedTokenReceiveWarningUseCase,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
    private val needShowYieldSupplyDepositedWarningUseCase: NeedShowYieldSupplyDepositedWarningUseCase,
    private val saveViewedYieldSupplyWarningUseCase: SaveViewedYieldSupplyWarningUseCase,
    private val isCryptoCurrencyCouldHideUseCase: IsCryptoCurrencyCouldHideUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val uiMessageSender: UiMessageSender,
) : BaseWalletClickIntents(), WalletCurrencyActionsClickIntents {

    override fun onSendClick(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonSend(
                token = cryptoCurrencyStatus.currency.symbol,
                status = unavailabilityReason.toReasonAnalyticsText(),
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        modelScope.launch(dispatchers.main) {
            if (needShowYieldSupplyWarning(cryptoCurrencyStatus)) {
                stateHolder.hideBottomSheet()
                router.openYieldSupplyBottomSheet(
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                    tokenAction = TokenAction.Send,
                    onWarningAcknowledged = { tokenAction ->
                        modelScope.launch {
                            saveViewedYieldSupplyWarningUseCase(cryptoCurrencyStatus.currency.name)
                            stateHolder.hideBottomSheet()
                            navigateToSend(cryptoCurrencyStatus, accountId.userWalletId)
                        }
                    },
                )
            } else {
                navigateToSend(cryptoCurrencyStatus, accountId.userWalletId)
            }
        }
    }

    override fun onReceiveClick(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        event: AnalyticsEvent?,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonReceive(
                token = cryptoCurrencyStatus.currency.symbol,
                status = AVAILABLE,
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        event?.let { analyticsEventHandler.send(it) }

        modelScope.launch(dispatchers.main) {
            if (needShowYieldSupplyWarning(cryptoCurrencyStatus)) {
                stateHolder.hideBottomSheet()
                router.openYieldSupplyBottomSheet(
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                    tokenAction = TokenAction.Receive,
                    onWarningAcknowledged = { tokenAction ->
                        modelScope.launch {
                            saveViewedTokenReceiveWarningUseCase(cryptoCurrencyStatus.currency.name)
                            saveViewedYieldSupplyWarningUseCase(cryptoCurrencyStatus.currency.name)
                            stateHolder.hideBottomSheet()
                            navigateToReceive(cryptoCurrencyStatus)
                        }
                    },
                )
            } else {
                navigateToReceive(cryptoCurrencyStatus)
            }
        }
    }

    override fun onCopyAddressLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus): TextReference? {
        val networkAddress = cryptoCurrencyStatus.value.networkAddress ?: return null
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val addresses = networkAddress.availableAddresses.mapToAddressModels(cryptoCurrency).toImmutableList()
        val defaultAddress = addresses.firstOrNull()?.value ?: return null

        vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
        clipboardManager.setText(text = defaultAddress, isSensitive = true)
        analyticsEventHandler.send(
            TokenReceiveNewAnalyticsEvent.ButtonCopyAddress(
                token = cryptoCurrency.symbol,
                blockchainName = cryptoCurrency.network.name,
                tokenReceiveSource = TokenReceiveCopyActionSource.Main,
            ),
        )
        return resourceReference(R.string.wallet_notification_address_copied)
    }

    override fun onCopyAddressClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = TokenReceiveNewAnalyticsEvent.ButtonCopyAddress(
                token = cryptoCurrencyStatus.currency.symbol,
                blockchainName = cryptoCurrencyStatus.currency.network.name,
                tokenReceiveSource = TokenReceiveCopyActionSource.Main,
            ),
        )

        modelScope.launch(dispatchers.main) {
            walletManagersFacade.getDefaultAddress(
                userWalletId = accountId.userWalletId,
                network = cryptoCurrencyStatus.currency.network,
            )?.let { address ->
                stateHolder.update(CloseBottomSheetTransformer(userWalletId = accountId.userWalletId))

                clipboardManager.setText(text = address, isSensitive = true)
                walletEventSender.send(event = WalletEvent.CopyAddress)
            }
        }
    }

    override fun onHideTokensClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrencyStatus.currency.symbol),
        )

        modelScope.launch(dispatchers.main) {
            val currency = cryptoCurrencyStatus.currency
            val canHide = isCryptoCurrencyCouldHideUseCase(
                userWalletId = accountId.userWalletId,
                cryptoCurrency = currency,
            )

            if (canHide) {
                uiMessageSender.send(
                    WalletAlertUM.hideTokenConfirm(cryptoCurrency = cryptoCurrencyStatus.currency) {
                        onPerformHideToken(accountId, cryptoCurrencyStatus)
                    },
                )
            } else {
                uiMessageSender.send(WalletAlertUM.unableHideToken(cryptoCurrency = cryptoCurrencyStatus.currency))
            }
        }
    }

    override fun onPerformHideToken(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus) {
        modelScope.launch(dispatchers.io) {
            manageCryptoCurrenciesUseCase(accountId = accountId, remove = cryptoCurrencyStatus.currency)
                .fold(
                    ifLeft = {
                        walletEventSender.send(
                            event = WalletEvent.ShowError(text = resourceReference(R.string.common_error)),
                        )
                    },
                    ifRight = {
                        router.dialogNavigation.dismiss()
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
                getOfframpUrlUseCase(
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    appCurrencyCode = getSelectedAppCurrencyUseCase.unwrap().code,
                ).onRight { url ->
                    urlOpener.openUrl(url)
                    analyticsEventHandler.send(OfframpAnalyticsEvent.ScreenOpened)
                }
            }
        }
    }

    override fun onBuyClick(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonWithParams.ButtonBuy(
                token = cryptoCurrencyStatus.currency.symbol,
                status = unavailabilityReason.toReasonAnalyticsText(),
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        appRouter.push(
            AppRoute.Onramp(
                userWalletId = accountId.userWalletId,
                currency = cryptoCurrencyStatus.currency,
                source = OnrampSource.TOKEN_LONG_TAP,
            ),
        )
    }

    override fun onSwapClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        accountId: AccountId,
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

        modelScope.launch(dispatchers.main) {
            if (needShowYieldSupplyWarning(cryptoCurrencyStatus)) {
                stateHolder.hideBottomSheet()
                router.openYieldSupplyBottomSheet(
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                    tokenAction = TokenAction.Swap,
                    onWarningAcknowledged = { tokenAction ->
                        modelScope.launch {
                            saveViewedYieldSupplyWarningUseCase(cryptoCurrencyStatus.currency.name)
                            stateHolder.hideBottomSheet()
                            navigateToSwap(cryptoCurrencyStatus, accountId.userWalletId)
                        }
                    },
                )
            } else {
                navigateToSwap(cryptoCurrencyStatus, accountId.userWalletId)
            }
        }
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
                    shouldShowPortfolio = false,
                    analyticsParams = AppRoute.MarketsTokenDetails.AnalyticsParams(
                        blockchain = cryptoCurrencyStatus.currency.network.name,
                        source = "Main",
                    ),
                ),
            )

            stateHolder.update(CloseBottomSheetTransformer(userWalletId = stateHolder.getSelectedWalletId()))
        }
    }

    override fun onStakeClick(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        option: StakingOption?,
    ) {
        stateHolder.update(CloseBottomSheetTransformer(userWalletId = accountId.userWalletId))

        val integrationId = option?.integrationId ?: return

        modelScope.launch {
            val cryptoCurrency = cryptoCurrencyStatus.currency

            appRouter.push(
                AppRoute.Staking(
                    userWalletId = accountId.userWalletId,
                    cryptoCurrency = cryptoCurrency,
                    integrationId = integrationId,
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
        when (val tokenListState = selectedWallet.tokensListState) {
            is WalletTokensListState.ContentState.Content -> checkSwapCryptoAvailability(
                tokenCount = tokenListState.items.count { it is TokensListItemUM.Token },
            )
            is WalletTokensListState.ContentState.PortfolioContent -> checkSwapCryptoAvailability(
                tokenCount = tokenListState.items.sumOf { it.tokens.count { it is TokensListItemUM.Token } },
            )
            WalletTokensListState.ContentState.Loading,
            WalletTokensListState.ContentState.Locked,
            WalletTokensListState.Empty,
            -> return
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
            statusFlow = rampStateManager.getExpressInitializationStatus(userWalletId),
            route = AppRoute.BuyCrypto(userWalletId = userWalletId),
            eventCreator = { MainScreenAnalyticsEvent.ButtonBuy(status = it, screenType = screenType) },
        )
    }

    private fun openExplorer() {
        val userWalletId = stateHolder.getSelectedWalletId()

        modelScope.launch(dispatchers.main) {
            val currencyStatus = singleAccountStatusListSupplier.unwrap(userWalletId) ?: return@launch

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
                currency = currency,
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
        val selectedWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        if (selectedWallet is UserWallet.Cold && isDemoCardUseCase(cardId = selectedWallet.cardId)) {
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

        uiMessageSender.send(DialogMessage(message = unavailabilityReason.getUnavailabilityReasonText()))

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
                onError = {
                    analyticsEventHandler.send(event = eventCreator(AnalyticsParam.Status.Error))
                    uiMessageSender.send(WalletAlertUM.unavailableOperation())
                },
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

    private fun handleLoading(eventCreator: (AnalyticsParam.Status) -> MainScreenAnalyticsEvent) {
        analyticsEventHandler.send(event = eventCreator(AnalyticsParam.Status.Pending))

        uiMessageSender.send(WalletAlertUM.providersStillLoading())
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

    private suspend fun configureReceiveAddresses(cryptoCurrencyStatus: CryptoCurrencyStatus): TokenReceiveConfig? {
        val userWalletId = stateHolder.getSelectedWalletId()
        return receiveAddressesFactory.create(
            status = cryptoCurrencyStatus,
            userWalletId = userWalletId,
        )
    }

    private suspend fun needShowYieldSupplyWarning(cryptoCurrencyStatus: CryptoCurrencyStatus): Boolean {
        return needShowYieldSupplyDepositedWarningUseCase(cryptoCurrencyStatus)
    }

    private fun navigateToSend(cryptoCurrencyStatus: CryptoCurrencyStatus, userWalletId: UserWalletId) {
        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWalletId))
        val route = AppRoute.Send(
            currency = cryptoCurrencyStatus.currency,
            userWalletId = userWalletId,
        )

        appRouter.push(route)
    }

    private fun navigateToSwap(cryptoCurrencyStatus: CryptoCurrencyStatus, userWalletId: UserWalletId) {
        appRouter.push(
            AppRoute.Swap(
                currencyFrom = cryptoCurrencyStatus.currency,
                userWalletId = userWalletId,
                screenSource = AnalyticsParam.ScreensSources.LongTap.value,
            ),
        )
    }

    private fun navigateToReceive(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        stateHolder.hideBottomSheet()
        modelScope.launch {
            configureReceiveAddresses(cryptoCurrencyStatus = cryptoCurrencyStatus)?.let {
                router.openTokenReceiveBottomSheet(it)
            }
        }
    }

    private fun checkSwapCryptoAvailability(tokenCount: Int) {
        if (tokenCount < 2) {
            analyticsEventHandler.send(event = MainScreenAnalyticsEvent.ButtonSwap(AnalyticsParam.Status.Error))
            uiMessageSender.send(WalletAlertUM.insufficientTokensCountForSwapping())
        }
    }
}