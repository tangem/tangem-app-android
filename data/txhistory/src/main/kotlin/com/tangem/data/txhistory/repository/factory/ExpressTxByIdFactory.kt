package com.tangem.data.txhistory.repository.factory

import com.tangem.data.common.converter.ExpressProviderConverter
import com.tangem.data.txhistory.repository.converter.ExpressOnrampConverter
import com.tangem.data.txhistory.repository.converter.ExpressSwapConverter
import com.tangem.data.txhistory.repository.converter.OnrampCurrencyConverter
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCurrencyEntity
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.ExpressTx
import javax.inject.Inject

/**
 * Builds the [ExpressTx] for a single by-id lookup (see [com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2.getExpressTxById]),
 * given at most one of [ExpressExchangeEntity] / [ExpressOnrampEntity] resolved by id and the shared
 * providers/fiat-currencies lookups.
 */
internal class ExpressTxByIdFactory @Inject constructor(
    private val expressTransactionAssetFactory: ExpressTransactionAssetFactory,
) {

    private val expressProviderConverter = ExpressProviderConverter()
    private val swapConverter = ExpressSwapConverter()
    private val onrampConverter = ExpressOnrampConverter()
    private val onrampCurrencyConverter = OnrampCurrencyConverter()

    suspend fun create(userWalletId: UserWalletId, outgoingAddresses: List<String>, sources: Sources): ExpressTx? {
        fun String.expressProvider() = sources.providers[this]?.let(expressProviderConverter::convert)
        fun String.onrampFiatCurrency() = sources.fiatCurrencies[this]?.let(onrampCurrencyConverter::convert)

        return when {
            sources.exchange != null -> {
                val exchange = sources.exchange
                val currencies = expressTransactionAssetFactory.create(
                    userWalletId = userWalletId,
                    outgoingSwaps = listOf(exchange),
                    incomingSwaps = emptyList(),
                    onramps = emptyList(),
                )
                val input = ExpressSwapConverter.Input(
                    entity = exchange,
                    provider = exchange.providerId.expressProvider(),
                    isOutgoing = outgoingAddresses.any { it.equals(exchange.fromAddress, ignoreCase = true) },
                    fromCurrency = currencies[exchange.from.toAssetId()],
                    toCurrency = currencies[exchange.to.toAssetId()],
                    refundCurrency = exchange.toRefundAssetId()?.let { currencies[it] },
                )
                swapConverter.convert(input)
            }
            sources.onramp != null -> {
                val onramp = sources.onramp
                val currencies = expressTransactionAssetFactory.create(
                    userWalletId = userWalletId,
                    outgoingSwaps = emptyList(),
                    incomingSwaps = emptyList(),
                    onramps = listOf(onramp),
                )
                val input = ExpressOnrampConverter.Input(
                    entity = onramp,
                    provider = onramp.providerId.expressProvider(),
                    toCurrency = currencies[onramp.to.toAssetId()],
                    fiatCurrency = onramp.fromCurrencyCode.onrampFiatCurrency(),
                )
                onrampConverter.convert(input)
            }
            else -> null
        }
    }

    /** At most one of [exchange] / [onramp] is non-null: the two tables' ids are disjoint. */
    data class Sources(
        val exchange: ExpressExchangeEntity?,
        val onramp: ExpressOnrampEntity?,
        val providers: Map<String, ExpressProviderEntity>,
        val fiatCurrencies: Map<String, OnrampCurrencyEntity>,
    )
}