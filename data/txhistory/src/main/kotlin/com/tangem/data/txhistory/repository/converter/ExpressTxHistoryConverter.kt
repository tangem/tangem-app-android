package com.tangem.data.txhistory.repository.converter

import com.tangem.data.txhistory.repository.factory.toRefundAssetId
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.express.models.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime
import java.math.BigDecimal
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId

/**
 * Maps an exchange entity into a swap [ExpressTx.Swap].
 *
 * Direction is driven by [Input.isOutgoing]: outgoing views the swap's `from` side (pay-in, joined to
 * on-chain by [ExpressExchangeEntity.payinHash]); incoming views the `to` side (payout, joined by
 * [ExpressExchangeEntity.payoutHash]).
 */
internal class ExpressSwapConverter : Converter<ExpressSwapConverter.Input, ExpressTx.Swap> {

    override fun convert(value: Input): ExpressTx.Swap = ExpressTx.Swap(
        tx = convertExchangeTransaction(value),
        isOutgoing = value.isOutgoing,
        txInfo = null,
    )

    data class Input(
        val entity: ExpressExchangeEntity,
        val provider: ExpressProvider?,
        val isOutgoing: Boolean,
        val fromCurrency: CryptoCurrency?,
        val toCurrency: CryptoCurrency?,
        val refundCurrency: CryptoCurrency?,
    )
}

internal class ExpressOnrampConverter : Converter<ExpressOnrampConverter.Input, ExpressTx.Onramp> {

    override fun convert(value: Input): ExpressTx.Onramp {
        val entity = value.entity
        val toActualAmount = (entity.to.actualAmount ?: entity.to.amount)?.toScaledBigDecimal(entity.to.decimals)
        return ExpressTx.Onramp(
            tx = OnrampTransaction(
                txId = entity.txId,
                status = ExpressOnrampStatus.fromRaw(entity.status),
                createdAtMillis = parseIsoMillis(entity.createdAt),
                provider = value.provider,
                payoutHash = entity.payoutHash,
                payoutAddress = entity.payoutAddress,
                fromFiat = Amount(
                    currencySymbol = entity.fromCurrencyCode,
                    value = entity.fromAmount.toScaledBigDecimal(entity.fromPrecision),
                    decimals = entity.fromPrecision,
                    type = AmountType.FiatType(code = value.country?.defaultCurrency?.unit ?: entity.fromCurrencyCode),
                ),
                toAsset = ExpressTransactionAsset(
                    id = ExpressAssetId(networkId = entity.to.network, contractAddress = entity.to.contractAddress),
                    amount = toActualAmount,
                    decimals = entity.to.decimals,
                    cryptoCurrency = value.toCurrency,
                ),
                country = value.country,
                externalTxUrl = entity.externalTxUrl,
                toAmount = entity.to.amount?.toScaledBigDecimal(entity.to.decimals),
                toActualAmount = entity.to.actualAmount?.toScaledBigDecimal(entity.to.decimals),
            ),
            txInfo = null,
        )
    }

    data class Input(
        val entity: ExpressOnrampEntity,
        val provider: ExpressProvider?,
        val toCurrency: CryptoCurrency? = null,
        val country: OnrampCountry? = null,
    )
}

private fun convertExchangeTransaction(value: ExpressSwapConverter.Input): ExchangeTransaction {
    val entity = value.entity
    val toActualAmount = (entity.to.actualAmount ?: entity.to.amount).toScaledBigDecimal(entity.to.decimals)
    return ExchangeTransaction(
        txId = entity.txId,
        status = ExpressExchangeStatus.fromRaw(entity.status),
        createdAtMillis = parseIsoMillis(entity.createdAt),
        provider = value.provider,
        payinHash = entity.payinHash,
        payoutHash = entity.payoutHash,
        fromAddress = entity.fromAddress,
        payoutAddress = entity.payoutAddress,
        fromAsset = ExpressTransactionAsset(
            id = ExpressAssetId(networkId = entity.from.network, contractAddress = entity.from.contractAddress),
            amount = entity.from.amount.toScaledBigDecimal(entity.from.decimals),
            decimals = entity.from.decimals,
            cryptoCurrency = value.fromCurrency,
        ),
        toAsset = ExpressTransactionAsset(
            id = ExpressAssetId(networkId = entity.to.network, contractAddress = entity.to.contractAddress),
            amount = toActualAmount,
            decimals = entity.to.decimals,
            cryptoCurrency = value.toCurrency,
        ),
        externalTxUrl = entity.externalTxUrl,
        payinAddress = entity.payinAddress,
        updatedAtMillis = parseIsoMillis(entity.updatedAt),
        refundAssetId = entity.toRefundAssetId(),
        refundCurrency = value.refundCurrency,

        fromAmount = entity.from.amount.toScaledBigDecimal(entity.from.decimals),
        toAmount = entity.to.amount.toScaledBigDecimal(entity.to.decimals),
        toActualAmount = entity.to.actualAmount?.toScaledBigDecimal(entity.to.decimals),
    )
}

private fun parseIsoMillis(iso: String): Long = DateTime.parse(iso).millis

/**
 * Parses a raw minimal-unit amount string from the express backend and scales it to the human-readable
 * value promised by [ExpressTransactionAsset.amount] (and the onramp fiat [Amount.value]).
 */
private fun String.toScaledBigDecimal(decimals: Int): BigDecimal =
    (this.toBigDecimalOrNull() ?: BigDecimal.ZERO).movePointLeft(decimals)

/**
 * Active (non-terminal) RAW status values passed to the DAO `observe…` queries so in-progress deals
 * stay visible beyond the time window. Derived from [ExpressExchangeStatus.isTerminal] /
 * [ExpressOnrampStatus.isTerminal] so the query set and the typed terminal classification never drift.
 */
internal object ExpressStatusMapper {

    val activeExchangeStatuses: List<String> =
        ExpressExchangeStatus.entries.filterNot { it.isTerminal }.map { it.raw }

    val activeOnrampStatuses: List<String> =
        ExpressOnrampStatus.entries.filterNot { it.isTerminal }.map { it.raw }
}