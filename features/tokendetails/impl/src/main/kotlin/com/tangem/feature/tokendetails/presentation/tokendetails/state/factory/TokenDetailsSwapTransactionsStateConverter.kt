package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.common.Provider
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.toDateFormat
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ExchangeStatusState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

internal class TokenDetailsSwapTransactionsStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    appCurrencyProvider: Provider<AppCurrency>,
) : Converter<Unit, PersistentList<SwapTransactionsState>> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val appCurrency = appCurrencyProvider()

    override fun convert(value: Unit): PersistentList<SwapTransactionsState> {
        return persistentListOf()
    }

    fun convert(
        savedTransactions: List<SavedSwapTransactionListModel>,
        cryptoStatusList: List<CryptoCurrencyStatus>,
    ): PersistentList<SwapTransactionsState> {
        val result = mutableListOf<SwapTransactionsState>()

        savedTransactions.forEach { swapCurrency ->
            val firstStatus = cryptoStatusList.first { it.currency.id.value == swapCurrency.fromCryptoCurrencyId }
            val secondStatus = cryptoStatusList.first { it.currency.id.value == swapCurrency.toCryptoCurrencyId }

            val (fromCurrency, toCurrency) = if (swapCurrency.fromCryptoCurrencyId == firstStatus.currency.id.value) {
                firstStatus to secondStatus
            } else {
                secondStatus to firstStatus
            }

            swapCurrency.transactions.forEach { transaction ->
                val toAmount = transaction.toCryptoAmount
                val fromAmount = transaction.fromCryptoAmount
                val toFiatAmount = toAmount.multiply(toCurrency.value.fiatRate)
                val fromFiatAmount = fromAmount.multiply(fromCurrency.value.fiatRate)
                val timestamp = transaction.timestamp
                result.add(
                    SwapTransactionsState(
                        txId = transaction.txId,
                        provider = transaction.provider,
                        txUrl = transaction.status?.txUrl,
                        timestamp = TextReference.Str("${timestamp.toDateFormat()}, ${timestamp.toTimeFormat()}"),
                        fiatSymbol = appCurrency.symbol,
                        statuses = MutableStateFlow(getStatuses(transaction.status?.status)),
                        activeStatus = MutableStateFlow(transaction.status?.status),
                        notification = MutableStateFlow(
                            getNotification(
                                transaction.status?.status,
                                transaction.status?.txUrl,
                            ),
                        ),
                        toCryptoCurrencyId = toCurrency.currency.id,
                        toCryptoAmount = BigDecimalFormatter.formatCryptoAmount(
                            cryptoAmount = toAmount,
                            cryptoCurrency = toCurrency.currency,
                        ),
                        toCryptoSymbol = toCurrency.currency.symbol,
                        toFiatAmount = getFiatAmount(toFiatAmount),
                        toCurrencyIcon = iconStateConverter.convert(toCurrency),
                        fromCryptoCurrencyId = fromCurrency.currency.id,
                        fromCryptoAmount = BigDecimalFormatter.formatCryptoAmount(
                            cryptoAmount = fromAmount,
                            cryptoCurrency = fromCurrency.currency,
                        ),
                        fromCryptoSymbol = fromCurrency.currency.symbol,
                        fromFiatAmount = getFiatAmount(fromFiatAmount),
                        fromCurrencyIcon = iconStateConverter.convert(fromCurrency),
                        onClick = { clickIntents.onSwapTransactionClick(transaction.txId) },
                        onGoToProviderClick = { clickIntents.onGoToProviderClick(transaction.status?.txUrl.orEmpty()) },
                    ),
                )
            }
        }
        return result.toPersistentList()
    }

    fun updateTxStatus(tx: SwapTransactionsState, status: ExchangeStatus?) {
        if (tx.activeStatus.value == status) return
        tx.activeStatus.update { status }
        tx.notification.update { getNotification(status, tx.txUrl) }
        tx.statuses.update { getStatuses(status) }
    }

    private fun getFiatAmount(toFiatAmount: BigDecimal): String {
        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = toFiatAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun getNotification(status: ExchangeStatus?, txUrl: String?): ExchangeStatusNotifications? {
        if (txUrl == null) return null
        return when (status) {
            ExchangeStatus.Failed -> ExchangeStatusNotifications.Failed {
                clickIntents.onGoToProviderClick(txUrl)
            }
            ExchangeStatus.Verifying -> ExchangeStatusNotifications.NeedVerification {
                clickIntents.onGoToProviderClick(txUrl)
            }
            else -> null
        }
    }

    private fun getStatuses(status: ExchangeStatus?): List<ExchangeStatusState> {
        val isWaiting = status == ExchangeStatus.New || status == ExchangeStatus.Waiting
        val isConfirming = status == ExchangeStatus.Confirming
        val isVerifying = status == ExchangeStatus.Verifying
        val isExchanging = status == ExchangeStatus.Exchanging
        val isFailed = status == ExchangeStatus.Failed
        val isSending = status == ExchangeStatus.Sending

        val isWaitingDone = !isWaiting
        val isConfirmingDone = !isConfirming && isWaitingDone
        val isExchangingDone = !isExchanging && isConfirmingDone
        val isSendingDone = !isSending && !isVerifying && !isFailed && isExchangingDone

        return listOf(
            waitStep(isWaiting, isWaitingDone),
            confirmStep(isConfirming, isConfirmingDone),
            exchangeStep(isExchanging, isExchangingDone, isVerifying, isFailed),
            sendStep(isSending, isSendingDone),
        )
    }

    private fun waitStep(isNew: Boolean, isNewDone: Boolean) = ExchangeStatusState(
        status = ExchangeStatus.New,
        text = when {
            isNew -> combinedReference(
                TextReference.Res(R.string.express_exchange_status_receiving),
                TextReference.Str(STATUS_ACTIVE_DOTS),
            )
            else -> TextReference.Res(R.string.express_exchange_status_receiving)
        },
        isActive = isNew,
        isDone = isNewDone,
    )

    private fun confirmStep(isConfirming: Boolean, isConfirmingDone: Boolean) = ExchangeStatusState(
        status = ExchangeStatus.Confirming,
        text = when {
            isConfirming -> combinedReference(
                TextReference.Res(R.string.express_exchange_status_confirming),
                TextReference.Str(STATUS_ACTIVE_DOTS),
            )
            isConfirmingDone -> TextReference.Res(R.string.express_exchange_status_confirmed)
            else -> TextReference.Res(R.string.express_exchange_status_confirming)
        },
        isActive = isConfirming,
        isDone = isConfirmingDone,
    )

    private fun exchangeStep(
        isExchanging: Boolean,
        isExchangingDone: Boolean,
        isVerifying: Boolean = false,
        isFailed: Boolean = false,
    ) = when {
        isVerifying -> ExchangeStatusState(
            status = ExchangeStatus.Verifying,
            text = TextReference.Res(R.string.express_exchange_status_verifying),
            isActive = true,
            isDone = false,
        )
        isFailed -> ExchangeStatusState(
            status = ExchangeStatus.Failed,
            text = TextReference.Res(R.string.express_exchange_status_failed),
            isActive = true,
            isDone = false,
        )
        else -> ExchangeStatusState(
            status = ExchangeStatus.Exchanging,
            text = when {
                isExchanging -> combinedReference(
                    TextReference.Res(R.string.express_exchange_status_exchanging),
                    TextReference.Str(STATUS_ACTIVE_DOTS),
                )
                isExchangingDone -> TextReference.Res(R.string.express_exchange_status_exchanged)
                else -> TextReference.Res(R.string.express_exchange_status_exchanging)
            },
            isActive = isExchanging,
            isDone = isExchangingDone,
        )
    }

    private fun sendStep(isSending: Boolean, isSendingDone: Boolean) = ExchangeStatusState(
        status = ExchangeStatus.Sending,
        text = when {
            isSending -> combinedReference(
                TextReference.Res(R.string.express_exchange_status_sending),
                TextReference.Str(STATUS_ACTIVE_DOTS),
            )
            isSendingDone -> TextReference.Res(R.string.express_exchange_status_sent)
            else -> TextReference.Res(R.string.express_exchange_status_sending)
        },
        isActive = isSending,
        isDone = isSendingDone,
    )

    private companion object {
        private const val STATUS_ACTIVE_DOTS = "…"
    }
}