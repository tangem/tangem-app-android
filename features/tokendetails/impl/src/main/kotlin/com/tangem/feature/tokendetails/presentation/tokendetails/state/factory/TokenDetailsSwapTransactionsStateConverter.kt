package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ExchangeStatusState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal
import java.util.Locale

internal class TokenDetailsSwapTransactionsStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val cryptoCurrency: CryptoCurrency,
    private val analyticsEventsHandlerProvider: Provider<AnalyticsEventHandler>,
    appCurrencyProvider: Provider<AppCurrency>,
) : Converter<Unit, PersistentList<SwapTransactionsState>> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val appCurrency = appCurrencyProvider()

    override fun convert(value: Unit): PersistentList<SwapTransactionsState> {
        return persistentListOf()
    }

    fun convert(
        savedTransactions: List<SavedSwapTransactionListModel>,
        quotes: Set<Quote>,
    ): PersistentList<SwapTransactionsState> {
        val result = mutableListOf<SwapTransactionsState>()

        savedTransactions
            .forEach { swapTransaction ->
                val toCryptoCurrency = swapTransaction.toCryptoCurrency
                val fromCryptoCurrency = swapTransaction.fromCryptoCurrency

                swapTransaction.transactions.forEach { transaction ->
                    val toAmount = transaction.toCryptoAmount
                    val fromAmount = transaction.fromCryptoAmount
                    val toFiatAmount = quotes.firstOrNull {
                        it.rawCurrencyId == swapTransaction.toCryptoCurrency.id.rawCurrencyId
                    }?.fiatRate?.multiply(toAmount)
                    val fromFiatAmount = quotes.firstOrNull {
                        it.rawCurrencyId == swapTransaction.fromCryptoCurrency.id.rawCurrencyId
                    }?.fiatRate?.multiply(fromAmount)
                    val timestamp = transaction.timestamp
                    val notifications =
                        getNotification(transaction.status?.status, transaction.status?.txExternalUrl, null)
                    val showProviderLink = getShowProviderLink(notifications, transaction.status)
                    result.add(
                        SwapTransactionsState(
                            txId = transaction.txId,
                            provider = transaction.provider,
                            txUrl = transaction.status?.txExternalUrl,
                            txExternalId = transaction.status?.txExternalId,
                            timestamp = TextReference.Str(
                                "${timestamp.toDateFormatWithTodayYesterday()}, ${timestamp.toTimeFormat()}",
                            ),
                            fiatSymbol = appCurrency.symbol,
                            statuses = getStatuses(transaction.status?.status),
                            hasFailed = transaction.status?.status == ExchangeStatus.Failed,
                            activeStatus = transaction.status?.status,
                            notification = notifications,
                            toCryptoCurrency = toCryptoCurrency,
                            toCryptoAmount = BigDecimalFormatter.formatCryptoAmount(
                                cryptoAmount = toAmount,
                                cryptoCurrency = toCryptoCurrency,
                            ),
                            toFiatAmount = getFiatAmount(toFiatAmount),
                            toCurrencyIcon = iconStateConverter.convert(toCryptoCurrency),
                            fromCryptoCurrency = fromCryptoCurrency,
                            fromCryptoAmount = BigDecimalFormatter.formatCryptoAmount(
                                cryptoAmount = fromAmount,
                                cryptoCurrency = fromCryptoCurrency,
                            ),
                            fromFiatAmount = getFiatAmount(fromFiatAmount),
                            fromCurrencyIcon = iconStateConverter.convert(fromCryptoCurrency),
                            showProviderLink = showProviderLink,
                            onClick = { clickIntents.onSwapTransactionClick(transaction.txId) },
                            onGoToProviderClick = { url ->
                                analyticsEventsHandlerProvider().send(
                                    TokenExchangeAnalyticsEvent.GoToProviderStatus(cryptoCurrency.symbol),
                                )
                                clickIntents.onGoToProviderClick(url = url)
                            },
                        ),
                    )
                }
            }
        return result.toPersistentList()
    }

    fun updateTxStatus(
        tx: SwapTransactionsState,
        statusModel: ExchangeStatusModel?,
        refundToken: CryptoCurrency?,
        isRefundTerminalStatus: Boolean,
    ): SwapTransactionsState {
        if (statusModel == null || tx.activeStatus == statusModel.status) return tx
        val hasFailed = tx.hasFailed || statusModel.status == ExchangeStatus.Failed
        val notifications = getNotification(statusModel.status, statusModel.txExternalUrl, refundToken)
        val showProviderLink = getShowProviderLink(notifications, statusModel)
        return tx.copy(
            activeStatus = statusModel.status,
            hasFailed = hasFailed,
            notification = notifications,
            statuses = getStatuses(statusModel.status, hasFailed),
            txUrl = statusModel.txExternalUrl,
            showProviderLink = showProviderLink,
            isRefundTerminalStatus = isRefundTerminalStatus,
        )
    }

    private fun getFiatAmount(toFiatAmount: BigDecimal?): String {
        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = toFiatAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun getNotification(
        status: ExchangeStatus?,
        txUrl: String?,
        refundToken: CryptoCurrency?,
    ): ExchangeStatusNotifications? {
        return when (status) {
            ExchangeStatus.Failed -> {
                if (txUrl == null) return null
                ExchangeStatusNotifications.Failed {
                    analyticsEventsHandlerProvider().send(
                        TokenExchangeAnalyticsEvent.GoToProviderFail(cryptoCurrency.symbol),
                    )
                    clickIntents.onGoToProviderClick(txUrl)
                }
            }
            ExchangeStatus.Verifying -> {
                if (txUrl == null) return null
                ExchangeStatusNotifications.NeedVerification {
                    analyticsEventsHandlerProvider().send(
                        TokenExchangeAnalyticsEvent.GoToProviderKYC(cryptoCurrency.symbol),
                    )
                    clickIntents.onGoToProviderClick(txUrl)
                }
            }
            ExchangeStatus.Refunded -> {
                if (refundToken == null) {
                    null
                } else {
                    ExchangeStatusNotifications.TokenRefunded(
                        cryptoCurrency = refundToken,
                        onReadMoreClick = { clickIntents.onOpenUrlClick(url = getAboutCrossChainBridgesLink()) },
                        onGoToTokenClick = { clickIntents.onGoToRefundedTokenClick(refundToken) },
                    )
                }
            }
            else -> null
        }
    }

    private fun getShowProviderLink(notifications: ExchangeStatusNotifications?, status: ExchangeStatusModel?) =
        notifications == null && status?.txExternalUrl != null && status.status != ExchangeStatus.Cancelled

    private fun getStatuses(status: ExchangeStatus?, hasFailed: Boolean = false): ImmutableList<ExchangeStatusState> {
        if (status == null) return persistentListOf()
        val isWaiting = status == ExchangeStatus.New || status == ExchangeStatus.Waiting
        val isWaitingTxHash = status == ExchangeStatus.WaitingTxHash
        val isConfirming = status == ExchangeStatus.Confirming
        val isVerifying = status == ExchangeStatus.Verifying
        val isExchanging = status == ExchangeStatus.Exchanging
        val isFailed = status == ExchangeStatus.Failed
        val isSending = status == ExchangeStatus.Sending
        val isRefunded = status == ExchangeStatus.Refunded

        val isWaitingDone = !isWaiting
        val isConfirmingDone = !isConfirming && isWaitingDone
        val isExchangingDone = !isExchanging && isConfirmingDone
        val isSendingDone = !isSending && !isVerifying && !isFailed && isExchangingDone

        return buildList {
            when {
                status == ExchangeStatus.Cancelled -> add(cancelledStep())
                isWaitingTxHash -> add(waitTxStep())
                // ExchangeStatus.Unknown is temporary added for 1inch
                status == ExchangeStatus.Unknown -> add(unknownStateStep())
                else -> {
                    add(waitStep(isWaiting, isWaitingDone))
                    add(confirmStep(isConfirming, isConfirmingDone))
                    add(
                        exchangeStep(
                            isExchanging = isExchanging,
                            isExchangingDone = isExchangingDone,
                            isRefunded = isRefunded,
                            hasFailed = hasFailed,
                            isVerifying = isVerifying,
                            isFailed = isFailed,
                        ),
                    )
                    add(
                        sendStep(
                            isSending = isSending,
                            isSendingDone = isSendingDone,
                            isRefunded = isRefunded,
                        ),
                    )
                }
            }
        }.toPersistentList()
    }

    private fun cancelledStep() = ExchangeStatusState(
        status = ExchangeStatus.Cancelled,
        text = TextReference.Res(R.string.express_exchange_status_canceled),
        isActive = true,
        isDone = true,
    )

    private fun waitStep(isNew: Boolean, isNewDone: Boolean) = ExchangeStatusState(
        status = ExchangeStatus.New,
        text = when {
            isNew -> TextReference.Res(R.string.express_exchange_status_receiving_active)
            isNewDone -> TextReference.Res(R.string.express_exchange_status_received)
            else -> TextReference.Res(R.string.express_exchange_status_receiving)
        },
        isActive = isNew,
        isDone = isNewDone,
    )

    private fun waitTxStep() = ExchangeStatusState(
        status = ExchangeStatus.Verifying,
        text = TextReference.Res(R.string.express_exchange_status_waiting_tx_hash),
        isActive = false,
        isDone = false,
    )

    private fun unknownStateStep() = ExchangeStatusState(
        status = ExchangeStatus.Failed,
        text = TextReference.Res(R.string.express_exchange_status_failed),
        isActive = false,
        isDone = true,
    )

    private fun confirmStep(isConfirming: Boolean, isConfirmingDone: Boolean) = ExchangeStatusState(
        status = ExchangeStatus.Confirming,
        text = when {
            isConfirming -> TextReference.Res(R.string.express_exchange_status_confirming_active)
            isConfirmingDone -> TextReference.Res(R.string.express_exchange_status_confirmed)
            else -> TextReference.Res(R.string.express_exchange_status_confirming)
        },
        isActive = isConfirming,
        isDone = isConfirmingDone,
    )

    private fun exchangeStep(
        isExchanging: Boolean,
        isExchangingDone: Boolean,
        isRefunded: Boolean,
        hasFailed: Boolean,
        isVerifying: Boolean = false,
        isFailed: Boolean = false,
    ) = when {
        isVerifying -> ExchangeStatusState(
            status = ExchangeStatus.Verifying,
            text = TextReference.Res(R.string.express_exchange_status_verifying),
            isActive = true,
            isDone = false,
        )
        hasFailed || isFailed || isRefunded -> ExchangeStatusState(
            status = ExchangeStatus.Failed,
            text = TextReference.Res(R.string.express_exchange_status_failed),
            isActive = isFailed || isRefunded,
            isDone = isRefunded,
        )
        else -> ExchangeStatusState(
            status = ExchangeStatus.Exchanging,
            text = when {
                isExchanging -> TextReference.Res(R.string.express_exchange_status_exchanging_active)
                isExchangingDone -> TextReference.Res(R.string.express_exchange_status_exchanged)
                else -> TextReference.Res(R.string.express_exchange_status_exchanging)
            },
            isActive = isExchanging,
            isDone = isExchangingDone,
        )
    }

    private fun sendStep(isSending: Boolean, isSendingDone: Boolean, isRefunded: Boolean) = when {
        isRefunded -> ExchangeStatusState(
            status = ExchangeStatus.Refunded,
            text = TextReference.Res(R.string.express_exchange_status_refunded),
            isActive = false,
            isDone = false,
        )
        else -> ExchangeStatusState(
            status = ExchangeStatus.Sending,
            text = when {
                isSending -> TextReference.Res(R.string.express_exchange_status_sending_active)
                isSendingDone -> TextReference.Res(R.string.express_exchange_status_sent)
                else -> TextReference.Res(R.string.express_exchange_status_sending)
            },
            isActive = isSending,
            isDone = isSendingDone,
        )
    }

    private fun getAboutCrossChainBridgesLink(): String {
        return if (Locale.getDefault().country == "RU") {
            "https://tangem.com/ru/blog/post/an-overview-of-cross-chain-bridges/"
        } else {
            "https://tangem.com/en/blog/post/an-overview-of-cross-chain-bridges/"
        }
    }
}
