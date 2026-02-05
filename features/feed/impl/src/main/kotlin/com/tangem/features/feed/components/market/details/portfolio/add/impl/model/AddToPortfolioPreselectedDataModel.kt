package com.tangem.features.feed.components.market.details.portfolio.add.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.markets.GetTokenMarketCryptoCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.feed.components.market.details.portfolio.add.*
import com.tangem.features.feed.components.market.details.portfolio.add.impl.AddTokenComponent
import com.tangem.features.feed.components.market.details.portfolio.impl.analytics.PortfolioAnalyticsEvent
import com.tangem.features.feed.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
internal class AddToPortfolioPreselectedDataModel @Inject constructor(
    paramsContainer: ParamsContainer,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    val portfolioSelectorController: PortfolioSelectorController,
    private val callbackDelegate: AddToPortfolioFromEarnCallbackDelegate,
    private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getTokenMarketCryptoCurrency: GetTokenMarketCryptoCurrency,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
) : Model(), AddTokenComponent.Callbacks by callbackDelegate {

    private val params = paramsContainer.require<AddToPortfolioPreselectedDataComponent.Params>()

    val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
        scope = modelScope,
    )

    val navigation = StackNavigation<AddToPortfolioRoutes>()
    var currentStack = listOf<AddToPortfolioRoutes>(AddToPortfolioRoutes.Empty)

    private val _selectedNetwork = MutableStateFlow<SelectedNetwork?>(null)
    val selectedNetwork: Flow<SelectedNetwork> = _selectedNetwork.asStateFlow().filterNotNull()

    private val _selectedPortfolio = MutableStateFlow<SelectedPortfolio?>(null)
    val selectedPortfolio: Flow<SelectedPortfolio> = _selectedPortfolio.asStateFlow().filterNotNull()
    val eventBuilder = PortfolioAnalyticsEvent.EventBuilder(
        tokenSymbol = params.tokenToAdd.symbol,
        source = AnalyticsParam.ScreensSources.Markets.value,
    )

    init {
        navigation.subscribe { currentStack = it.transformer.invoke(currentStack) }
        startAddToPortfolioFlow()
    }

    @Suppress("LongMethod")
    private fun startAddToPortfolioFlow() {
        channelFlow<Unit> {
            fun finishSuccessFlow(currency: CryptoCurrency, userWalletId: UserWalletId) {
                params.callback.onSuccess(addedToken = currency, walletId = userWalletId)
                channel.close()
            }

            val data = createAvailableToAddDataForPreselectedNetwork(params.tokenToAdd.network)
                ?: return@channelFlow

            val isAccountMode = portfolioSelectorController.isAccountModeSync()
            val firstSelectedPortfolio = setupPortfolioFlow(data)
                .onEach { _selectedPortfolio.value = it }

            val firstSelectedNetwork = firstSelectedPortfolio
                .map { portfolio -> createSelectedNetwork(network = params.tokenToAdd.network, portfolio = portfolio) }
                .filterNotNull()
                .onEach { _selectedNetwork.value = it }

            val isSinglePortfolio = data.isSinglePortfolio
            if (isSinglePortfolio) {
                val accountId = data.availableToAddWallets.values.first()
                    .availableToAddAccounts.values.first()
                    .account.account.accountId
                // force select a portfolio, triggers [selectedPortfolio]
                portfolioSelectorController.selectAccount(accountId)
            } else {
                logAccountSelector(isAccountMode)
                navigation.replaceAll(AddToPortfolioRoutes.PortfolioSelector)
            }

            // main flow that combine all require data
            val allRequireForAdd = combine(
                flow = firstSelectedNetwork,
                flow2 = firstSelectedPortfolio,
                transform = { a, b -> a to b },
            )

            // suspend until all required data is selected
            val (selectedNetworkValue, selectedPortfolioValue) = allRequireForAdd.first()

            val isTokenAlreadyAdded = getAccountCurrencyStatusUseCase.invokeSync(
                userWalletId = selectedPortfolioValue.userWallet.walletId,
                currency = selectedNetworkValue.cryptoCurrency,
            ).isSome()

            if (isTokenAlreadyAdded) {
                finishSuccessFlow(
                    currency = selectedNetworkValue.cryptoCurrency,
                    userWalletId = selectedPortfolioValue.userWallet.walletId,
                )
                return@channelFlow
            }

            navigation.replaceAll(AddToPortfolioRoutes.AddToken)
            val addedToken = callbackDelegate.onTokenAdded.receiveAsFlow().first()
            messageSender.send(ToastMessage(message = resourceReference(R.string.markets_token_added)))
            finishSuccessFlow(addedToken.currency, selectedPortfolioValue.userWallet.walletId)
        }
            .catch { throwable ->
                Timber.e(throwable)
                params.callback.onDismiss()
            }
            .launchIn(modelScope)
    }

    private fun logAccountSelector(isAccountMode: Boolean) {
        if (isAccountMode) {
            analyticsEventHandler.send(eventBuilder.popupToChooseAccount())
        }
    }

    private fun minimalTokenMarketParams() = with(params.tokenToAdd) {
        TokenMarketParams(
            id = id,
            name = name,
            symbol = symbol,
            tokenQuotes = TokenMarketParams.Quotes(
                currentPrice = BigDecimal.ZERO,
                h24Percent = null,
                weekPercent = null,
                monthPercent = null,
            ),
            imageUrl = null,
        )
    }

    /**
     * Creates [AvailableToAddData] for preselected network when token is already added in all networks.
     * This allows user to select wallet/account and do smth after it with selected info.
     */
    private suspend fun createAvailableToAddDataForPreselectedNetwork(
        preSelectedNetwork: TokenMarketInfo.Network,
    ): AvailableToAddData? {
        val portfolioData = portfolioFetcher.data.firstOrNull() ?: return null

        val availableToAddInWallets = portfolioData.balances.mapNotNull { (walletId, balance) ->
            val wallet = balance.userWallet
            val accounts = balance.accountsBalance.accountStatuses

            val availableToAddAccounts = accounts.mapNotNull { accountStatus ->
                val accountIndex = when (accountStatus) {
                    is AccountStatus.CryptoPortfolio -> accountStatus.account.derivationIndex
                }

                val cryptoCurrency = getTokenMarketCryptoCurrency(
                    userWalletId = walletId,
                    tokenMarketParams = minimalTokenMarketParams(),
                    network = preSelectedNetwork,
                    accountIndex = accountIndex,
                ) ?: return@mapNotNull null

                val addedNetworks = getAccountCurrencyStatusUseCase.invokeSync(
                    userWalletId = walletId,
                    currency = cryptoCurrency,
                ).fold(
                    ifEmpty = { emptySet() },
                    ifSome = { setOf(it.status.currency.network) },
                )

                AvailableToAddAccount(
                    account = accountStatus,
                    availableNetworks = setOf(preSelectedNetwork),
                    addedNetworks = addedNetworks,
                )
            }.associateBy { it.account.account.accountId }

            if (availableToAddAccounts.isEmpty()) return@mapNotNull null

            walletId to AvailableToAddWallet(
                userWallet = wallet,
                accounts = accounts,
                availableNetworks = setOf(preSelectedNetwork),
                availableToAddAccounts = availableToAddAccounts,
            )
        }.toMap()

        if (availableToAddInWallets.isEmpty()) return null

        return AvailableToAddData(availableToAddWallets = availableToAddInWallets)
    }

    private suspend fun createCryptoCurrency(
        userWallet: UserWallet,
        network: TokenMarketInfo.Network,
        account: AvailableToAddAccount,
    ): CryptoCurrency? {
        val accountIndex = when (val accountStatus = account.account) {
            is AccountStatus.CryptoPortfolio -> accountStatus.account.derivationIndex
        }
        return getTokenMarketCryptoCurrency(
            userWalletId = userWallet.walletId,
            tokenMarketParams = minimalTokenMarketParams(),
            network = network,
            accountIndex = accountIndex,
        )
    }

    private suspend fun createSelectedNetwork(
        network: TokenMarketInfo.Network,
        portfolio: SelectedPortfolio,
    ): SelectedNetwork? {
        val cryptoCurrency = createCryptoCurrency(
            userWallet = portfolio.userWallet,
            network = network,
            account = portfolio.account,
        ) ?: return null

        return SelectedNetwork(
            cryptoCurrency = cryptoCurrency,
            selectedNetwork = network,
            isAvailableMoreNetwork = false,
        )
    }

    private fun setupPortfolioFlow(data: AvailableToAddData): Flow<SelectedPortfolio> = combine(
        flow = portfolioSelectorController.isAccountMode,
        flow2 = portfolioSelectorController.selectedAccount,
        transform = { isAccountMode, selectedAccountId ->
            selectedAccountId ?: return@combine null
            val availableToAddWallets =
                data.availableToAddWallets[selectedAccountId.userWalletId] ?: return@combine null
            val availableToAddAccount =
                availableToAddWallets.availableToAddAccounts[selectedAccountId] ?: return@combine null
            if (!isAccountMode) analyticsEventHandler.send(eventBuilder.addToPortfolioWalletChanged())
            SelectedPortfolio(
                isAccountMode = isAccountMode,
                userWallet = availableToAddWallets.userWallet,
                account = availableToAddAccount,
                isAvailableMorePortfolio = false,
            )
        },
    )
        .filterNotNull()
}

@ModelScoped
internal class AddToPortfolioFromEarnCallbackDelegate @Inject constructor() :
    AddTokenComponent.Callbacks {
    val onTokenAdded = Channel<CryptoCurrencyStatus>()

    override fun onChangeNetworkClick() = Unit

    override fun onChangePortfolioClick() = Unit

    override fun onTokenAdded(status: CryptoCurrencyStatus) {
        onTokenAdded.trySend(status)
    }
}