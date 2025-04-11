package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.swaptx.ExpressAnalyticsStatus
import com.tangem.datasource.local.swaptx.SwapTransactionStatusStore
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.quotes.QuotesRepositoryV2
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsSwapTransactionsStateConverter
import com.tangem.utils.Provider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

@Suppress("LongParameterList")
internal class ExchangeStatusFactory @AssistedInject constructor(
    private val swapTransactionRepository: SwapTransactionRepository,
    private val swapRepository: SwapRepository,
    private val quotesRepository: QuotesRepository,
    private val quotesRepositoryV2: QuotesRepositoryV2,
    private val tokensFeatureToggles: TokensFeatureToggles,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val swapTransactionStatusStore: SwapTransactionStatusStore,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    @Assisted private val clickIntents: TokenDetailsClickIntents,
    @Assisted private val appCurrencyProvider: Provider<AppCurrency>,
    @Assisted private val currentStateProvider: Provider<TokenDetailsState>,
    @Assisted private val userWalletId: UserWalletId,
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

    suspend operator fun invoke(): Flow<PersistentList<ExchangeUM>> {
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
                    ?.toSet()
                    ?.getQuotesOrEmpty(true)
                    ?: emptySet()

                getExchangeStatusState(
                    savedTransactions = savedTransactions,
                    quotes = quotes,
                )
            }
    }

    suspend fun removeTransactionOnBottomSheetClosed(isForceDispose: Boolean = false) {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetConfig?.content as? ExpressStatusBottomSheetConfig ?: return
        val selectedTx = bottomSheetConfig.value as? ExchangeUM ?: return

        val shouldDispose = selectedTx.activeStatus?.isAutoDisposable == true || isForceDispose
        if (shouldDispose) {
            swapTransactionRepository.removeTransaction(
                userWalletId = userWalletId,
                fromCryptoCurrency = selectedTx.fromCryptoCurrency,
                toCryptoCurrency = selectedTx.toCryptoCurrency,
                txId = selectedTx.info.txId,
            )
        }
    }

    suspend fun updateSwapTxStatus(swapTx: ExchangeUM): ExchangeUM {
        return if (swapTx.activeStatus?.isTerminal == true) {
            swapTx
        } else {
            val statusModel = getExchangeStatus(swapTx.info.txId, swapTx.provider)

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

    private suspend fun getExchangeStatus(txId: String, provider: SwapProvider): ExchangeStatusModel? {
        return swapRepository.getExchangeStatus(txId)
            .fold(
                ifLeft = { null },
                ifRight = { statusModel ->
                    sendStatusUpdateAnalytics(statusModel, provider)

                    val refundTokenCurrency = addRefundCurrencyIfNeeded(statusModel, provider.type)

                    swapTransactionRepository.storeTransactionState(txId, statusModel, refundTokenCurrency)
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
    ): PersistentList<ExchangeUM> {
        if (savedTransactions == null) {
            return persistentListOf()
        }

        return swapTransactionsStateConverter.convert(
            savedTransactions = savedTransactions,
            quotes = quotes,
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

    private suspend fun Set<CryptoCurrency.ID>.getQuotesOrEmpty(refresh: Boolean): Set<Quote> {
        return try {
            val rawIds = mapNotNull { it.rawCurrencyId }.toSet()

            if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
                quotesRepositoryV2.getMultiQuoteSyncOrNull(currenciesIds = rawIds).orEmpty()
            } else {
                quotesRepository.getQuotesSync(rawIds, refresh)
            }
        } catch (t: Throwable) {
            emptySet()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            clickIntents: TokenDetailsClickIntents,
            appCurrencyProvider: Provider<AppCurrency>,
            currentStateProvider: Provider<TokenDetailsState>,
            userWalletId: UserWalletId,
            cryptoCurrency: CryptoCurrency,
        ): ExchangeStatusFactory
    }
}