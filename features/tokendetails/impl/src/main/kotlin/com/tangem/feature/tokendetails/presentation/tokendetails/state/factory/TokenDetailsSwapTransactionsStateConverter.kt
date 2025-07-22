package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateIconUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateInfoUM
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
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.quote.mapData
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus.Companion.isFailed
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeStatusState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
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

// Fixme [REDACTED_JIRA]
@Suppress("LargeClass")
internal class TokenDetailsSwapTransactionsStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val cryptoCurrency: CryptoCurrency,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    appCurrencyProvider: Provider<AppCurrency>,
) : Converter<Unit, PersistentList<ExchangeUM>> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val appCurrency = appCurrencyProvider()

    override fun convert(value: Unit): PersistentList<ExchangeUM> {
        return persistentListOf()
    }

    fun convert(
        savedTransactions: List<SavedSwapTransactionListModel>,
        quoteStatuses: Set<QuoteStatus>,
    ): PersistentList<ExchangeUM> {
        val result = mutableListOf<ExchangeUM>()

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
                    quoteStatuses.forEach { quote ->
                        quote.mapData {
                            if (quote.rawCurrencyId == toCryptoCurrencyRawId) {
                                toFiatAmount = fiatRate.multiply(toAmount)
                            }
                        }

                        quote.mapData {
                            if (quote.rawCurrencyId == fromCryptoCurrencyRawId) {
                                fromFiatAmount = fiatRate.multiply(fromAmount)
                            }
                        }
                    }
                    val statusModel = transaction.status
                    val notification = getNotification(
                        status = statusModel?.status,
                        txUrl = statusModel?.txExternalUrl,
                        refundToken = statusModel?.refundCurrency,
                        hasLongTime = statusModel?.hasLongTime,
                    )
                    val showProviderLink = getShowProviderLink(notification, transaction.status)
                    result.add(
                        ExchangeUM(
                            provider = transaction.provider,
                            statuses = getStatuses(statusModel?.status),
                            notification = notification,
                            activeStatus = statusModel?.status,
                            showProviderLink = showProviderLink,
                            fromCryptoCurrency = fromCryptoCurrency,
                            toCryptoCurrency = toCryptoCurrency,
                            info = createStateInfo(
                                transaction,
                                toCryptoCurrency,
                                fromCryptoCurrency,
                                toFiatAmount,
                                fromFiatAmount,
                            ),
                            hasLongTime = transaction.status?.hasLongTime ?: false,
                        ),
                    )
                }
            }
        return result.toPersistentList()
    }

    fun updateTxStatus(tx: ExchangeUM, statusModel: ExchangeStatusModel): ExchangeUM {
        if (tx.activeStatus == statusModel.status && tx.hasLongTime == statusModel.hasLongTime) {
            Timber.e("UpdateTxStatus isn't required. Current status isn't changed")
            return tx
        }
        val hasFailed = statusModel.status.isFailed()
        val notification = getNotification(
            status = statusModel.status,
            txUrl = statusModel.txExternalUrl,
            refundToken = statusModel.refundCurrency,
            hasLongTime = statusModel.hasLongTime,
        )
        val showProviderLink = getShowProviderLink(notification, statusModel)
        return tx.copy(
            activeStatus = statusModel.status,
            notification = notification,
            statuses = getStatuses(statusModel.status, hasFailed),
            showProviderLink = showProviderLink,
            info = tx.info.copy(
                txExternalId = statusModel.txExternalId,
                txExternalUrl = statusModel.txExternalUrl,
            ),
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
            notification = null, // fixme [REDACTED_JIRA],
            onDisposeExpressStatus = clickIntents::onConfirmDisposeExpressStatus,
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
        hasLongTime: Boolean?,
    ): ExchangeStatusNotification? {
        return when {
            status == ExchangeStatus.Failed || status == ExchangeStatus.TxFailed -> {
                if (txUrl == null) return null
                ExchangeStatusNotification.Failed {
                    analyticsEventsHandler.send(
                        TokenExchangeAnalyticsEvent.GoToProviderFail(cryptoCurrency.symbol),
                    )
                    clickIntents.onGoToProviderClick(txUrl)
                }
            }
            status == ExchangeStatus.Verifying -> {
                if (txUrl == null) return null
                ExchangeStatusNotification.NeedVerification {
                    analyticsEventsHandler.send(
                        TokenExchangeAnalyticsEvent.GoToProviderKYC(cryptoCurrency.symbol),
                    )
                    clickIntents.onGoToProviderClick(txUrl)
                }
            }
            status == ExchangeStatus.Refunded -> {
                if (refundToken == null) {
                    null
                } else {
                    ExchangeStatusNotification.TokenRefunded(
                        cryptoCurrency = refundToken,
                        onReadMoreClick = { clickIntents.onOpenUrlClick(url = getAboutCrossChainBridgesLink()) },
                        onGoToTokenClick = { clickIntents.onGoToRefundedTokenClick(refundToken) },
                    )
                }
            }
            status?.isTerminal == false && txUrl != null && hasLongTime == true -> {
                ExchangeStatusNotification.LongTimeExchange {
                    analyticsEventsHandler.send(
                        event = TokenExchangeAnalyticsEvent.GoToProviderLongTime(cryptoCurrency.symbol),
                    )
                    clickIntents.onGoToProviderClick(txUrl)
                }
            }
            else -> null
        }
    }

    private fun getIconState(status: ExchangeStatus?): ExpressTransactionStateIconUM {
        return when (status) {
            ExchangeStatus.Verifying -> ExpressTransactionStateIconUM.Warning
            ExchangeStatus.Failed,
            ExchangeStatus.TxFailed,
            ExchangeStatus.Cancelled,
            -> ExpressTransactionStateIconUM.Error
            else -> ExpressTransactionStateIconUM.None
        }
    }

    // Fixme [REDACTED_JIRA]
    private fun getStatusState() = ExpressStatusUM(
        title = resourceReference(R.string.express_exchange_status_title),
        link = ExpressLinkUM.Empty,
        statuses = persistentListOf(),
    )

    private fun getShowProviderLink(notifications: ExchangeStatusNotification?, status: ExchangeStatusModel?) =
        notifications == null && status?.txExternalUrl != null && status.status != ExchangeStatus.Cancelled

    private fun getStatuses(status: ExchangeStatus?, hasFailed: Boolean = false): ImmutableList<ExchangeStatusState> {
        if (status == null) return persistentListOf()
        val isWaiting = status == ExchangeStatus.New || status == ExchangeStatus.Waiting
        val isWaitingTxHash = status == ExchangeStatus.WaitingTxHash
        val isConfirming = status == ExchangeStatus.Confirming
        val isVerifying = status == ExchangeStatus.Verifying
        val isExchanging = status == ExchangeStatus.Exchanging
        val isFailed = status.isFailed()
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