package com.tangem.feature.wallet.child.wallet.model.intents

import androidx.compose.ui.geometry.Offset
import arrow.core.getOrElse
import com.tangem.utils.logging.TangemLogger
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.analytics.NFTAnalyticsEvent
import com.tangem.domain.settings.ShouldShowMarketsTooltipUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplySetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.OnrampStatusFactory
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.OpenBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.MultiWalletCurrencyActionsConverter
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
internal interface WalletContentClickIntents {

    fun onDetailsClick()

    fun onOrganizeTokensClick()

    fun onDismissMarketsTooltip()

    fun onTokenItemClick(accountId: AccountId, currencyStatus: CryptoCurrencyStatus)

    fun onTokenItemLongClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onTokenItemLongClickV2(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        offset: Offset,
        tokenRowUM: TangemTokenRowUM,
    )

    fun onYieldPromoCloseClick()

    fun onYieldPromoShown(cryptoCurrency: CryptoCurrency)

    fun onYieldPromoClicked(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus, apy: String)

    fun onAccountExpandClick(account: Account)

    fun onAccountCollapseClick(account: Account)

    fun onManageTokensClick(accountId: AccountId)

    fun onManageTokensClick(userWalletId: UserWalletId)

    fun onTransactionClick(txHash: String)

    fun onDissmissBottomSheet()

    fun onGoToProviderClick(externalTxUrl: String)

    fun onExpressTransactionClick(txId: String)

    fun onConfirmDisposeExpressStatus()

    fun onDisposeExpressStatus()

    fun onNFTClick(userWallet: UserWallet)

    fun onScanQrClick()
}

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
@ModelScoped
internal class WalletContentClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val currencyActionsClickIntents: WalletCurrencyActionsClickIntentsImplementor,
    private val onrampStatusFactory: OnrampStatusFactory,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
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

    override fun onTokenItemClick(accountId: AccountId, currencyStatus: CryptoCurrencyStatus) {
        router.openTokenDetails(accountId.userWalletId, currencyStatus)
    }

    override fun onTokenItemLongClick(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus) {
        modelScope.launch(dispatchers.main) {
            val userWalletId = accountId.userWalletId
            val userWallet = getUserWalletUseCase(userWalletId).getOrElse { error ->
                TangemLogger.e(
                    """
                        Unable to get user wallet
                        |- ID: $userWalletId
                        |- Exception: $error
                    """.trimIndent(),
                )

                return@launch
            }

            getCryptoCurrencyActionsUseCase(userWallet = userWallet, cryptoCurrencyStatus = cryptoCurrencyStatus)
                .take(count = 1)
                .collectLatest { actionsState ->
                    router.openTokenActionSheet(
                        userWallet = userWallet,
                        tokenActionList = MultiWalletCurrencyActionsConverter(
                            userWallet = userWallet,
                            accountId = accountId,
                            clickIntents = currencyActionsClickIntents,
                        ).convert(actionsState),
                        offset = Offset.Zero,
                        tokenRowUM = null,
                    )
                }
        }
    }

    override fun onTokenItemLongClickV2(
        accountId: AccountId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        offset: Offset,
        tokenRowUM: TangemTokenRowUM,
    ) {
        modelScope.launch {
            val userWalletId = accountId.userWalletId
            val userWallet = getUserWalletUseCase(userWalletId).getOrElse { exception ->
                TangemLogger.e(
                    """
                        Unable to get user wallet
                        |- ID: $userWalletId
                        |- Exception: $exception
                    """.trimIndent(),
                )

                return@launch
            }

            getCryptoCurrencyActionsUseCase(userWallet = userWallet, cryptoCurrencyStatus = cryptoCurrencyStatus)
                .take(count = 1)
                .collectLatest { actionsState ->
                    router.openTokenActionSheet(
                        userWallet = userWallet,
                        tokenActionList = MultiWalletCurrencyActionsConverter(
                            userWallet = userWallet,
                            accountId = accountId,
                            clickIntents = currencyActionsClickIntents,
                        ).convert(actionsState)
                            .filter { it.isEnabled }
                            .toPersistentList(),
                        offset = offset,
                        tokenRowUM = tokenRowUM,
                    )
                }
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

    override fun onYieldPromoClicked(accountId: AccountId, cryptoCurrencyStatus: CryptoCurrencyStatus, apy: String) {
        analyticsEventHandler.send(
            MainScreenAnalyticsEvent.YieldPromoClicked(
                token = cryptoCurrencyStatus.currency.symbol,
                blockchain = cryptoCurrencyStatus.currency.network.name,
            ),
        )
        router.openYieldSupplyEntryScreen(
            userWalletId = accountId.userWalletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
            apy = apy,
        )
    }

    override fun onAccountExpandClick(account: Account) {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.AccountShowTokens())
        accountDependencies.expandedAccountsHolder.expandAccount(account.accountId)
        walletEventSender.send(WalletEvent.CollapseBalance)
    }

    override fun onAccountCollapseClick(account: Account) {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.AccountHideTokens())
        accountDependencies.expandedAccountsHolder.collapseAccount(account.accountId)
    }

    override fun onManageTokensClick(accountId: AccountId) {
        router.openManageTokensScreen(accountId)
    }

    override fun onManageTokensClick(userWalletId: UserWalletId) {
        router.openManageTokensScreen(
            AccountId.forMainCryptoPortfolio(userWalletId),
        )
    }

    override fun onTransactionClick(txHash: String) {
        modelScope.launch(dispatchers.main) {
            val currency = singleAccountStatusListSupplier.unwrap(
                userWalletId = stateHolder.getSelectedWalletId(),
            )
                ?.currency
                ?: return@launch

            getExplorerTransactionUrlUseCase(
                txHash = txHash,
                currency = currency,
            ).fold(
                ifLeft = { TangemLogger.e(it.toString()) },
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

    override fun onScanQrClick() {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.ButtonQrScan())
        router.openQrScanner()
    }
}