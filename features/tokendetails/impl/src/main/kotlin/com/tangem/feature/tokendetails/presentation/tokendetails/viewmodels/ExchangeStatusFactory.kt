package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.swaptx.ExchangeAnalyticsStatus
import com.tangem.datasource.local.swaptx.SwapTransactionStatusStore
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.models.analytics.TokenExchangeAnalyticsEvent
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsSwapTransactionsStateConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange.ExchangeStatusBottomSheetConfig
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class ExchangeStatusFactory(
    private val swapTransactionRepository: SwapTransactionRepository,
    private val swapRepository: SwapRepository,
    private val quotesRepository: QuotesRepository,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val swapTransactionStatusStore: SwapTransactionStatusStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val clickIntents: TokenDetailsClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val analyticsEventsHandlerProvider: Provider<AnalyticsEventHandler>,
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val userWalletId: UserWalletId,
    private val cryptoCurrency: CryptoCurrency,
) {

    private val swapTransactionsStateConverter by lazy {
        TokenDetailsSwapTransactionsStateConverter(
            clickIntents = clickIntents,
            cryptoCurrency = cryptoCurrency,
            appCurrencyProvider = appCurrencyProvider,
            analyticsEventsHandlerProvider = analyticsEventsHandlerProvider,
        )
    }

    suspend operator fun invoke(): Flow<PersistentList<SwapTransactionsState>> {
        val selectedWallet = getSelectedWalletSyncUseCase().fold(
            ifLeft = { return emptyFlow() },
            ifRight = { it },
        )
        return swapTransactionRepository.getTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyId = cryptoCurrency.id,
            scanResponse = selectedWallet.scanResponse,
        ).conflate()
            .map { savedTransactions ->
                val quotes = savedTransactions
                    ?.flatMap { setOf(it.fromCryptoCurrency.id, it.toCryptoCurrency.id) }
                    ?.let { quotesRepository.getQuotesSync(it.toSet(), true) }
                    ?: emptySet()

                getExchangeStatusState(
                    savedTransactions = savedTransactions,
                    quotes = quotes,
                )
            }
    }

    suspend fun removeTransactionOnBottomSheetClosed(): TokenDetailsState {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetConfig?.content as? ExchangeStatusBottomSheetConfig ?: return state
        val selectedTx = bottomSheetConfig.value

        return if (selectedTx.activeStatus.isTerminal(selectedTx.isRefundTerminalStatus)) {
            swapTransactionRepository.removeTransaction(
                userWalletId = userWalletId,
                fromCryptoCurrency = selectedTx.fromCryptoCurrency,
                toCryptoCurrency = selectedTx.toCryptoCurrency,
                txId = selectedTx.txId,
            )
            val filteredTxs = state.swapTxs
                .filterNot { it.txId == selectedTx.txId }
                .toPersistentList()
            state.copy(swapTxs = filteredTxs)
        } else {
            state
        }
    }

    suspend fun updateSwapTxStatuses(swapTxList: PersistentList<SwapTransactionsState>) = withContext(dispatchers.io) {
        swapTxList.map { tx ->
            async {
                val statusModel = getExchangeStatus(tx.txId)
                val isRefundTerminalStatus = statusModel?.refundNetwork == null &&
                    statusModel?.refundContractAddress == null &&
                    tx.provider.type != ExchangeProviderType.DEX_BRIDGE
                if (tx.activeStatus.isTerminal(isRefundTerminalStatus)) {
                    tx
                } else {
                    val addedRefundToken = addRefundCurrencyIfNeeded(statusModel, tx.provider.type)
                    swapTransactionsStateConverter.updateTxStatus(
                        tx = tx,
                        statusModel = statusModel,
                        refundToken = addedRefundToken,
                        isRefundTerminalStatus = isRefundTerminalStatus,
                    )
                }
            }
        }
            .awaitAll()
            .toPersistentList()
    }

    private suspend fun getExchangeStatus(txId: String): ExchangeStatusModel? {
        return swapRepository.getExchangeStatus(txId)
            .fold(
                ifLeft = { null },
                ifRight = { statusModel ->
                    sendStatusUpdateAnalytics(statusModel)
                    swapTransactionRepository.storeTransactionState(txId, statusModel)
                    statusModel
                },
            )
    }

    private suspend fun sendStatusUpdateAnalytics(statusModel: ExchangeStatusModel) {
        val txId = statusModel.txId ?: return
        val status = toAnalyticStatus(statusModel.status) ?: return
        val savedStatus = swapTransactionStatusStore.getTransactionStatus(txId)

        if (savedStatus != status) {
            analyticsEventsHandlerProvider().send(
                TokenExchangeAnalyticsEvent.CexTxStatusChanged(cryptoCurrency.symbol, status.value),
            )
            swapTransactionStatusStore.setTransactionStatus(txId, status)
        }
    }

    /**
     * For now do it only for dex-bridge provider
     */
    private suspend fun addRefundCurrencyIfNeeded(
        status: ExchangeStatusModel?,
        type: ExchangeProviderType,
    ): CryptoCurrency? {
        status ?: return null
        if (type != ExchangeProviderType.DEX_BRIDGE) return null
        val refundNetwork = status.refundNetwork
        val refundContractAddress = status.refundContractAddress
        if (refundNetwork != null && refundContractAddress != null) {
            return addCryptoCurrenciesUseCase(
                userWalletId = userWalletId,
                contractAddress = refundContractAddress,
                networkId = refundNetwork,
            ).getOrNull()
        }
        return null
    }

    private fun getExchangeStatusState(
        savedTransactions: List<SavedSwapTransactionListModel>?,
        quotes: Set<Quote>,
    ): PersistentList<SwapTransactionsState> {
        if (savedTransactions == null) {
            return persistentListOf()
        }

        return swapTransactionsStateConverter.convert(
            savedTransactions = savedTransactions,
            quotes = quotes,
        )
    }

    private fun ExchangeStatus?.isTerminal(isRefundTerminal: Boolean): Boolean {
        val needTerminalRefund = this == ExchangeStatus.Refunded && isRefundTerminal
        return needTerminalRefund ||
            this == ExchangeStatus.Finished ||
            this == ExchangeStatus.Cancelled ||
            this == ExchangeStatus.TxFailed ||
            this == ExchangeStatus.Unknown
    }

    private fun toAnalyticStatus(status: ExchangeStatus?): ExchangeAnalyticsStatus? {
        return when (status) {
            ExchangeStatus.New,
            ExchangeStatus.Waiting,
            ExchangeStatus.Sending,
            ExchangeStatus.Confirming,
            ExchangeStatus.Exchanging,
            -> ExchangeAnalyticsStatus.InProgress
            ExchangeStatus.WaitingTxHash -> ExchangeAnalyticsStatus.WaitingTxHash
            ExchangeStatus.Verifying -> ExchangeAnalyticsStatus.KYC
            ExchangeStatus.Failed -> ExchangeAnalyticsStatus.Fail
            ExchangeStatus.TxFailed -> ExchangeAnalyticsStatus.FailTx
            ExchangeStatus.Finished -> ExchangeAnalyticsStatus.Done
            ExchangeStatus.Refunded -> ExchangeAnalyticsStatus.Refunded
            ExchangeStatus.Cancelled -> ExchangeAnalyticsStatus.Cancelled
            ExchangeStatus.Unknown -> ExchangeAnalyticsStatus.Unknown
            else -> null
        }
    }
}
