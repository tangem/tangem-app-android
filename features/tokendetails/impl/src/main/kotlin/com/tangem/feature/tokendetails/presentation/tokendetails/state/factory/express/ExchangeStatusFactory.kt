package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.swap.ExpressAnalyticsStatus
import com.tangem.datasource.local.swap.SwapTransactionStatusStore
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsSwapTransactionsStateConverter
import com.tangem.utils.Provider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

@Suppress("LongParameterList")
internal class ExchangeStatusFactory @AssistedInject constructor(
    private val swapTransactionRepository: SwapTransactionRepository,
    private val swapRepository: SwapRepository,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val swapTransactionStatusStore: SwapTransactionStatusStore,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    @Assisted private val clickIntents: ExpressTransactionsClickIntents,
    @Assisted private val appCurrencyProvider: Provider<AppCurrency>,
    @Assisted private val currentStateProvider: Provider<ExpressTransactionsBlockState>,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val cryptoCurrency: CryptoCurrency,
) {

    private val swapTransactionsStateConverter by lazy {
        TokenDetailsSwapTransactionsStateConverter(
            clickIntents = clickIntents,
            cryptoCurrency = cryptoCurrency,
            appCurrencyProvider = appCurrencyProvider,
            analyticsEventsHandler = analyticsEventsHandler,
        )
    }

    operator fun invoke(): Flow<PersistentList<ExchangeUM>> {
        return swapTransactionRepository.getTransactions(
            userWallet = userWallet,
            cryptoCurrencyId = cryptoCurrency.id,
        ).conflate()
            .map { savedTransactions ->
                val accountStatuses = savedTransactions
                    ?.flatMapTo(mutableSetOf()) { swapTransaction ->
                        listOf(
                            swapTransaction.fromAccount to swapTransaction.fromCryptoCurrency,
                            swapTransaction.toAccount to swapTransaction.toCryptoCurrency,
                        )
                    }
                    ?.getStatuses()
                    .orEmpty()

                getExchangeStatusState(
                    savedTransactions = savedTransactions,
                    accountStatuses = accountStatuses,
                )
            }
    }

    suspend fun removeTransactionOnBottomSheetClosed(isForceDispose: Boolean = false) {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetSlot?.config?.content as? ExpressStatusBottomSheetConfig ?: return
        val selectedTx = bottomSheetConfig.value as? ExchangeUM ?: return

        val shouldDispose = selectedTx.activeStatus?.isAutoDisposable == true || isForceDispose
        if (shouldDispose) {
            swapTransactionRepository.removeTransaction(
                userWalletId = userWallet.walletId,
                txId = selectedTx.info.txId,
            )
        }
    }

    suspend fun updateSwapTxStatus(swapTx: ExchangeUM): ExchangeUM {
        return if (swapTx.activeStatus?.isTerminal == true) {
            swapTx
        } else {
            val statusModel = getExchangeStatus(swapTx.info.txId, swapTx.provider, swapTx.fromUserWalletId)

            if (statusModel != null) {
                swapTransactionsStateConverter.updateTxStatus(
                    tx = swapTx,
                    statusModel = statusModel,
                )
            } else {
                swapTx
            }
        }
    }

    private suspend fun getExchangeStatus(
        txId: String,
        provider: SwapProvider,
        fromUserWalletId: UserWalletId,
    ): ExchangeStatusModel? {
        val fromUserWallet = getUserWalletUseCase(fromUserWalletId).getOrNull()
        return swapRepository.getExchangeStatus(
            userWallet = fromUserWallet,
            userWalletId = fromUserWalletId,
            txId = txId,
        )
            .fold(
                ifLeft = { null },
                ifRight = { statusModel ->
                    sendStatusUpdateAnalytics(statusModel, provider)

                    val accountId = getAccountCurrencyStatusUseCase.invokeSync(
                        userWalletId = fromUserWalletId,
                        currency = cryptoCurrency,
                    )
                        .map { it.account.accountId }
                        .getOrNull()

                    val refundTokenCurrency = if (accountId != null) {
                        addRefundCurrencyIfNeeded(
                            accountId = accountId,
                            status = statusModel,
                            type = provider.type,
                        )
                    } else {
                        TangemLogger.e("Account ID is null, cannot add refund currency ${cryptoCurrency.id}")
                        null
                    }

                    swapTransactionRepository.storeTransactionState(
                        txId = txId,
                        status = statusModel,
                        accountWithCurrency = if (refundTokenCurrency != null) {
                            Pair(accountId, refundTokenCurrency)
                        } else {
                            null
                        },
                    )
                    statusModel.copy(refundCurrency = refundTokenCurrency)
                },
            )
    }

    private suspend fun sendStatusUpdateAnalytics(statusModel: ExchangeStatusModel, provider: SwapProvider) {
        val txId = statusModel.txId ?: return
        val status = toAnalyticStatus(statusModel.status) ?: return
        val savedStatus = swapTransactionStatusStore.getTransactionStatus(txId)

        if (savedStatus != status) {
            analyticsEventsHandler.send(
                TokenExchangeAnalyticsEvent.CexTxStatusChanged(cryptoCurrency.symbol, status.value, provider.name),
            )
            swapTransactionStatusStore.setTransactionStatus(txId, status)
        }
    }

    private suspend fun addRefundCurrencyIfNeeded(
        accountId: AccountId,
        status: ExchangeStatusModel?,
        type: ExchangeProviderType,
    ): CryptoCurrency? {
        status ?: return null
        if (type != ExchangeProviderType.DEX_BRIDGE) return null
        val refundNetwork = status.refundNetwork
        val refundContractAddress = status.refundContractAddress

        if (refundNetwork == null || refundContractAddress == null) return null

        return manageCryptoCurrenciesUseCase.add(
            accountId = accountId,
            contractAddress = refundContractAddress,
            networkId = refundNetwork,
        )
            .onLeft { TangemLogger.e("Error", it) }
            .getOrNull()
    }

    private fun getExchangeStatusState(
        savedTransactions: List<SavedSwapTransactionListModel>?,
        accountStatuses: Map<Account, List<CryptoCurrencyStatus>>,
    ): PersistentList<ExchangeUM> {
        if (savedTransactions == null) {
            return persistentListOf()
        }

        return swapTransactionsStateConverter.convert(
            savedTransactions = savedTransactions,
            accountStatuses = accountStatuses,
        )
    }

    private fun toAnalyticStatus(status: ExchangeStatus?): ExpressAnalyticsStatus? {
        return when (status) {
            ExchangeStatus.New,
            ExchangeStatus.Waiting,
            ExchangeStatus.Sending,
            ExchangeStatus.Confirming,
            ExchangeStatus.Exchanging,
            -> ExpressAnalyticsStatus.InProgress
            ExchangeStatus.WaitingTxHash -> ExpressAnalyticsStatus.WaitingTxHash
            ExchangeStatus.Verifying -> ExpressAnalyticsStatus.KYC
            ExchangeStatus.Failed -> ExpressAnalyticsStatus.Fail
            ExchangeStatus.TxFailed -> ExpressAnalyticsStatus.FailTx
            ExchangeStatus.Finished -> ExpressAnalyticsStatus.Done
            ExchangeStatus.Refunded -> ExpressAnalyticsStatus.Refunded
            ExchangeStatus.Cancelled -> ExpressAnalyticsStatus.Cancelled
            ExchangeStatus.Unknown -> ExpressAnalyticsStatus.Unknown
            else -> null
        }
    }

    private suspend fun Set<Pair<Account?, CryptoCurrency>>.getStatuses(): Map<Account, List<CryptoCurrencyStatus>> {
        return mapNotNull { (account, cryptoCurrency) ->
            when (account) {
                is Account.CryptoPortfolio -> {
                    val (cryptoPortfolioAccount, currencyStatus) = getAccountCurrencyStatusUseCase.invokeSync(
                        userWalletId = account.userWalletId,
                        currency = cryptoCurrency,
                    ).getOrNull() ?: return@mapNotNull null

                    cryptoPortfolioAccount to currencyStatus
                }
                is Account.Payment -> getPaymentAccountCryptoCurrencyStatusUseCase.invokeSync(
                    userWalletId = account.userWalletId,
                    cryptoCurrency = cryptoCurrency,
                ).getOrNull()
                else -> null
            }
        }.groupBy(
            keySelector = { (account, _) -> account },
            valueTransform = { (_, status) -> status },
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            clickIntents: ExpressTransactionsClickIntents,
            appCurrencyProvider: Provider<AppCurrency>,
            currentStateProvider: Provider<ExpressTransactionsBlockState>,
            userWallet: UserWallet,
            cryptoCurrency: CryptoCurrency,
        ): ExchangeStatusFactory
    }
}