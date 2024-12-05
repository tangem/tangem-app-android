package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeStatusState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateIconUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateInfoUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateUM
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale

// Fixme https://tangem.atlassian.net/browse/AND-9219
@Suppress("LargeClass")
internal class TokenDetailsSwapTransactionsStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val cryptoCurrency: CryptoCurrency,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    appCurrencyProvider: Provider<AppCurrency>,
) : Converter<Unit, PersistentList<ExpressTransactionStateUM.ExchangeUM>> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val appCurrency = appCurrencyProvider()

    override fun convert(value: Unit): PersistentList<ExpressTransactionStateUM.ExchangeUM> {
        return persistentListOf()
    }

    fun convert(
        savedTransactions: List<SavedSwapTransactionListModel>,
        quotes: Set<Quote>,
    ): PersistentList<ExpressTransactionStateUM.ExchangeUM> {
        val result = mutableListOf<ExpressTransactionStateUM.ExchangeUM>()

        savedTransactions
            .forEach { swapTransaction ->
                val toCryptoCurrency = swapTransaction.toCryptoCurrency
                val fromCryptoCurrency = swapTransaction.fromCryptoCurrency
                val toCryptoCurrencyRawId = swapTransaction.toCryptoCurrency.id.rawCurrencyId
                val fromCryptoCurrencyRawId = swapTransaction.fromCryptoCurrency.id.rawCurrencyId

                swapTransaction.transactions.forEach { transaction ->
                    val toAmount = transaction.toCryptoAmount
                    val fromAmount = transaction.fromCryptoAmount
                    var toFiatAmount: BigDecimal? = null
                    var fromFiatAmount: BigDecimal? = null
                    quotes.forEach { quote ->
                        if (quote is Quote.Value && quote.rawCurrencyId == toCryptoCurrencyRawId) {
                            toFiatAmount = quote.fiatRate.multiply(toAmount)
                        }
                        if (quote is Quote.Value && quote.rawCurrencyId == fromCryptoCurrencyRawId) {
                            fromFiatAmount = quote.fiatRate.multiply(fromAmount)
                        }
                    }
                    val notifications =
                        getNotification(transaction.status?.status, transaction.status?.txExternalUrl, null)
                    val showProviderLink = getShowProviderLink(notifications, transaction.status)
                    result.add(
                        ExpressTransactionStateUM.ExchangeUM(
                            provider = transaction.provider,
                            statuses = getStatuses(transaction.status?.status),
                            notification = notifications,
                            activeStatus = transaction.status?.status,
                            showProviderLink = showProviderLink,
                            isRefundTerminalStatus = true,
                            fromCryptoCurrency = fromCryptoCurrency,
                            toCryptoCurrency = toCryptoCurrency,
                            info = createStateInfo(
                                transaction,
                                toCryptoCurrency,
                                fromCryptoCurrency,
                                toFiatAmount,
                                fromFiatAmount,
                            ),
                        ),
                    )
                }
            }
        return result.toPersistentList()
    }

    fun updateTxStatus(
        tx: ExpressTransactionStateUM.ExchangeUM,
        statusModel: ExchangeStatusModel?,
        refundToken: CryptoCurrency?,
        isRefundTerminalStatus: Boolean,
    ): ExpressTransactionStateUM.ExchangeUM {
        if (statusModel == null || tx.activeStatus == statusModel.status) {
            Timber.e("UpdateTxStatus isn't required. Current status isn't changed")
            return tx
        }
        val hasFailed = statusModel.status == ExchangeStatus.Failed
        val notifications = getNotification(statusModel.status, statusModel.txExternalUrl, refundToken)
        val showProviderLink = getShowProviderLink(notifications, statusModel)
        return tx.copy(
            activeStatus = statusModel.status,
            notification = notifications,
            statuses = getStatuses(statusModel.status, hasFailed),
            showProviderLink = showProviderLink,
            isRefundTerminalStatus = isRefundTerminalStatus,
            info = tx.info.copy(txExternalUrl = statusModel.txExternalUrl),
        )
    }

    private fun createStateInfo(
        transaction: SavedSwapTransactionModel,
        toCryptoCurrency: CryptoCurrency,
        fromCryptoCurrency: CryptoCurrency,
        toFiatAmount: BigDecimal?,
        fromFiatAmount: BigDecimal?,
    ): ExpressTransactionStateInfoUM {
        val timestamp = transaction.timestamp
        return ExpressTransactionStateInfoUM(
            title = resourceReference(R.string.express_exchange_by, wrappedList(transaction.provider.name)),
            txId = transaction.txId,
            txExternalUrl = transaction.status?.txExternalUrl,
            txExternalId = transaction.status?.txExternalId,
            timestamp = timestamp,
            timestampFormatted = stringReference(
                "${timestamp.toDateFormatWithTodayYesterday()}, ${timestamp.toTimeFormat()}",
            ),
            toAmount = getCryptoAmount(transaction.toCryptoAmount, toCryptoCurrency),
            toFiatAmount = getFiatAmount(toFiatAmount),
            toCurrencyIcon = iconStateConverter.convert(toCryptoCurrency),
            toAmountSymbol = toCryptoCurrency.symbol,
            fromAmount = getCryptoAmount(transaction.fromCryptoAmount, fromCryptoCurrency),
            fromFiatAmount = getFiatAmount(fromFiatAmount),
            fromCurrencyIcon = iconStateConverter.convert(fromCryptoCurrency),
            fromAmountSymbol = fromCryptoCurrency.symbol,
            onClick = { clickIntents.onExpressTransactionClick(transaction.txId) },
            onGoToProviderClick = { url ->
                analyticsEventsHandler.send(
                    TokenExchangeAnalyticsEvent.GoToProviderStatus(cryptoCurrency.symbol),
                )
                clickIntents.onGoToProviderClick(url = url)
            },
            iconState = getIconState(transaction.status?.status),
            status = getStatusState(),
            notification = null, // fixme https://tangem.atlassian.net/browse/AND-9219
        )
    }

    private fun getCryptoAmount(amount: BigDecimal?, cryptoCurrency: CryptoCurrency) = stringReference(
        amount.format { crypto(cryptoCurrency) },
    )

    private fun getFiatAmount(toFiatAmount: BigDecimal?) = stringReference(
        toFiatAmount.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        },
    )

    private fun getNotification(
        status: ExchangeStatus?,
        txUrl: String?,
        refundToken: CryptoCurrency?,
    ): ExchangeStatusNotifications? {
        return when (status) {
            ExchangeStatus.Failed -> {
                if (txUrl == null) return null
                ExchangeStatusNotifications.Failed {
                    analyticsEventsHandler.send(
                        TokenExchangeAnalyticsEvent.GoToProviderFail(cryptoCurrency.symbol),
                    )
                    clickIntents.onGoToProviderClick(txUrl)
                }
            }
            ExchangeStatus.Verifying -> {
                if (txUrl == null) return null
                ExchangeStatusNotifications.NeedVerification {
                    analyticsEventsHandler.send(
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

    private fun getIconState(status: ExchangeStatus?): ExpressTransactionStateIconUM {
        return when (status) {
            ExchangeStatus.Verifying -> ExpressTransactionStateIconUM.Warning
            ExchangeStatus.Failed, ExchangeStatus.Cancelled -> ExpressTransactionStateIconUM.Error
            else -> ExpressTransactionStateIconUM.None
        }
    }

    // Fixme https://tangem.atlassian.net/browse/AND-9219
    private fun getStatusState() = ExpressStatusUM(
        title = resourceReference(R.string.express_exchange_status_title),
        link = ExpressLinkUM.Empty,
        statuses = persistentListOf(),
    )

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
        val isPaused = status == ExchangeStatus.Paused

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
                            isPaused = isPaused,
                        ),
                    )
                    if (!isPaused) {
                        add(
                            sendStep(
                                isSending = isSending,
                                isSendingDone = isSendingDone,
                                isRefunded = isRefunded,
                            ),
                        )
                    }
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
        isPaused: Boolean,
    ) = when {
        isVerifying -> ExchangeStatusState(
            status = ExchangeStatus.Verifying,
            text = TextReference.Res(R.string.express_exchange_status_verifying),
            isActive = true,
            isDone = false,
        )
        isPaused -> ExchangeStatusState(
            status = ExchangeStatus.Paused,
            text = resourceReference(id = R.string.express_exchange_status_paused),
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
