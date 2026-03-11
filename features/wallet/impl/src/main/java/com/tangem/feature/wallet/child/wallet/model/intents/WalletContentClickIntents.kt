package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.getOrElse
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.tokens.TokenItemStateConverter.ApySource
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.analytics.NFTAnalyticsEvent
import com.tangem.domain.settings.ShouldShowMarketsTooltipUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplySetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.OnrampStatusFactory
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.OpenBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.MultiWalletCurrencyActionsConverter
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface WalletContentClickIntents {

    fun onDetailsClick()

    fun onOrganizeTokensClick()

    fun onDismissMarketsTooltip()

    fun onTokenItemClick(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus)

    fun onTokenItemLongClick(userWalletId: UserWalletId, cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onApyLabelClick(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        apySource: ApySource,
        apy: String,
    )

    fun onYieldPromoCloseClick()

    fun onYieldPromoShown(cryptoCurrency: CryptoCurrency)

    fun onYieldPromoClicked(cryptoCurrency: CryptoCurrency)

    fun onAccountExpandClick(account: Account)

    fun onAccountCollapseClick(account: Account)

    fun onTransactionClick(txHash: String)

    fun onDissmissBottomSheet()

    fun onGoToProviderClick(externalTxUrl: String)

    fun onExpressTransactionClick(txId: String)

    fun onConfirmDisposeExpressStatus()

    fun onDisposeExpressStatus()

    fun onNFTClick(userWallet: UserWallet)
}

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class WalletContentClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val currencyActionsClickIntents: WalletCurrencyActionsClickIntentsImplementor,
    private val onrampStatusFactory: OnrampStatusFactory,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val shouldShowMarketsTooltipUseCase: ShouldShowMarketsTooltipUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletEventSender: WalletEventSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val accountDependencies: AccountDependencies,
    private val yieldSupplySetShouldShowMainPromoUseCase: YieldSupplySetShouldShowMainPromoUseCase,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
    private val uiMessageSender: UiMessageSender,
) : BaseWalletClickIntents(), WalletContentClickIntents {

    override fun onDetailsClick() {
        router.openDetailsScreen(stateHolder.getSelectedWalletId())
    }

    override fun onOrganizeTokensClick() {
        router.openOrganizeTokensScreen(userWalletId = stateHolder.getSelectedWalletId())
    }

    override fun onDismissMarketsTooltip() {
        stateHolder.update { it.copy(showMarketsOnboarding = false) }
        modelScope.launch {
            shouldShowMarketsTooltipUseCase(isShown = true)
        }
    }

    override fun onTokenItemClick(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus) {
        router.openTokenDetails(userWalletId, currencyStatus)
    }

    override fun onTokenItemLongClick(userWalletId: UserWalletId, cryptoCurrencyStatus: CryptoCurrencyStatus) {
        modelScope.launch(dispatchers.main) {
            val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
                Timber.e(
                    """
                        Unable to get user wallet
                        |- ID: $userWalletId
                        |- Exception: $it
                    """.trimIndent(),
                )

                return@launch
            }

            getCryptoCurrencyActionsUseCase(userWallet = userWallet, cryptoCurrencyStatus = cryptoCurrencyStatus)
                .take(count = 1)
                .collectLatest {
                    showActionsBottomSheet(it, userWallet)
                }
        }
    }

    override fun onApyLabelClick(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        apySource: ApySource,
        apy: String,
    ) {
        val navigationAction = when (apySource) {
            ApySource.STAKING -> NavigationAction.Staking
            ApySource.YIELD_SUPPLY -> {
                NavigationAction.YieldSupply(currencyStatus.value.yieldSupplyStatus?.isActive == true)
            }
        }

        sendApyLabelClickAnalytics(navigationAction, currencyStatus)

        when (navigationAction) {
            is NavigationAction.Staking -> router.openTokenDetails(userWalletId, currencyStatus, navigationAction)
            is NavigationAction.YieldSupply -> openYieldSupply(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = currencyStatus,
                apy = apy,
            )
            is NavigationAction.CloreMigration -> Unit
        }
    }

    override fun onYieldPromoCloseClick() {
        modelScope.launch {
            yieldSupplySetShouldShowMainPromoUseCase(false)
        }
    }

    override fun onYieldPromoShown(cryptoCurrency: CryptoCurrency) {
        modelScope.launch(dispatchers.io) {
            tokenListAnalyticsSender.sendYieldPromoShown(
                userWalletId = stateHolder.getSelectedWalletId(),
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            )
        }
    }

    override fun onYieldPromoClicked(cryptoCurrency: CryptoCurrency) {
        modelScope.launch(dispatchers.io) {
            analyticsEventHandler.send(
                MainScreenAnalyticsEvent.YieldPromoClicked(
                    token = cryptoCurrency.symbol,
                    blockchain = cryptoCurrency.network.name,
                ),
            )
        }
    }

    override fun onAccountExpandClick(account: Account) {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.AccountShowTokens())
        accountDependencies.expandedAccountsHolder.expandAccount(account.accountId)
    }

    override fun onAccountCollapseClick(account: Account) {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.AccountHideTokens())
        accountDependencies.expandedAccountsHolder.collapseAccount(account.accountId)
    }

    private fun openYieldSupply(userWalletId: UserWalletId, cryptoCurrencyStatus: CryptoCurrencyStatus, apy: String) {
        router.openYieldSupplyEntryScreen(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
            apy = apy,
        )
    }

    private fun sendApyLabelClickAnalytics(navigationAction: NavigationAction, currencyStatus: CryptoCurrencyStatus) {
        val event = when (navigationAction) {
            is NavigationAction.YieldSupply -> {
                MainScreenAnalyticsEvent.ApyClicked(
                    token = currencyStatus.currency.symbol,
                    blockchain = currencyStatus.currency.network.name,
                    action = "Earning",
                    state = if (currencyStatus.value.yieldSupplyStatus?.isActive == true) {
                        "Enabled"
                    } else {
                        "Disabled"
                    },
                )
            }
            is NavigationAction.Staking -> {
                MainScreenAnalyticsEvent.ApyClicked(
                    token = currencyStatus.currency.symbol,
                    blockchain = currencyStatus.currency.network.name,
                    action = "Staking",
                    state = if (currencyStatus.value.stakingBalance is StakingBalance.Data) {
                        "Enabled"
                    } else {
                        "Disabled"
                    },
                )
            }
            is NavigationAction.CloreMigration -> return
        }
        analyticsEventHandler.send(event)
    }

    private fun showActionsBottomSheet(tokenActionsState: TokenActionsState, userWallet: UserWallet) {
        stateHolder.showBottomSheet(
            ActionsBottomSheetConfig(
                actions = MultiWalletCurrencyActionsConverter(
                    userWallet = userWallet,
                    clickIntents = currencyActionsClickIntents,
                ).convert(tokenActionsState),
            ),
            userWallet.walletId,
        )
    }

    override fun onTransactionClick(txHash: String) {
        modelScope.launch(dispatchers.main) {
            val currency = getSingleCryptoCurrencyStatusUseCase.unwrap(
                userWalletId = stateHolder.getSelectedWalletId(),
            )
                ?.currency
                ?: return@launch

            getExplorerTransactionUrlUseCase(
                txHash = txHash,
                currency = currency,
            ).fold(
                ifLeft = { Timber.e(it.toString()) },
                ifRight = { router.openUrl(url = it) },
            )
        }
    }

    override fun onGoToProviderClick(externalTxUrl: String) {
        router.openUrl(externalTxUrl)
    }

    override fun onDissmissBottomSheet() {
        val userWalletId = stateHolder.getSelectedWalletId()
        if (stateHolder.getSelectedWallet().bottomSheetConfig?.content is ExpressStatusBottomSheetConfig) {
            modelScope.launch(dispatchers.main) {
                onrampStatusFactory.removeTransactionOnBottomSheetClosed(forceDispose = false)
            }
        }
        stateHolder.update(CloseBottomSheetTransformer(userWalletId))
    }

    override fun onExpressTransactionClick(txId: String) {
        modelScope.launch {
            val userWalletId = stateHolder.getSelectedWalletId()
            val singleWalletState = stateHolder.getSelectedWallet() as? WalletState.SingleCurrency.Content
                ?: return@launch

            val expressTransaction = singleWalletState.expressTxsToDisplay.firstOrNull { it.info.txId == txId }
                ?: return@launch

            stateHolder.update(
                OpenBottomSheetTransformer(
                    userWalletId = userWalletId,
                    content = ExpressStatusBottomSheetConfig(
                        value = expressTransaction,
                    ),
                    onDismissBottomSheet = ::onDissmissBottomSheet,
                ),
            )
        }
    }

    override fun onConfirmDisposeExpressStatus() {
        uiMessageSender.send(
            WalletAlertUM.confirmExpressStatusHide(
                onConfirmClick = {
                    walletEventSender.onConsume()
                    onDisposeExpressStatus()
                },
            ),
        )
    }

    override fun onDisposeExpressStatus() {
        val userWalletId = stateHolder.getSelectedWalletId()
        if (stateHolder.getSelectedWallet().bottomSheetConfig?.content is ExpressStatusBottomSheetConfig) {
            modelScope.launch(dispatchers.main) {
                onrampStatusFactory.removeTransactionOnBottomSheetClosed(forceDispose = true)
            }
        }
        stateHolder.update(CloseBottomSheetTransformer(userWalletId))
    }

    override fun onNFTClick(userWallet: UserWallet) {
        val selectedWallet = stateHolder.getSelectedWallet() as? WalletState.MultiCurrency.Content ?: return
        when (val state = selectedWallet.nftState) {
            is WalletNFTItemUM.Content -> {
                analyticsEventHandler.send(
                    NFTAnalyticsEvent.NFTListScreenOpened(
                        state = AnalyticsParam.EmptyFull.Full,
                        allAssetsCount = state.allAssetsCount,
                        collectionsCount = state.collectionsCount,
                        noCollectionAssetsCount = state.noCollectionAssetsCount,
                    ),
                )
            }
            is WalletNFTItemUM.Empty -> {
                analyticsEventHandler.send(
                    NFTAnalyticsEvent.NFTListScreenOpened(
                        state = AnalyticsParam.EmptyFull.Empty,
                        allAssetsCount = 0,
                        collectionsCount = 0,
                        noCollectionAssetsCount = 0,
                    ),
                )
            }
            is WalletNFTItemUM.Failed,
            is WalletNFTItemUM.Hidden,
            is WalletNFTItemUM.Loading,
            -> Unit
        }

        router.openNFT(userWallet)
    }
}