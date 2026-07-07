package com.tangem.data.txhistory.repository.converter

import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Maps an exchange entity into a swap [ExpressTx.Swap].
 *
 * Direction is driven by [Input.isOutgoing]: outgoing views the swap's `from` side (pay-in, joined to
 * on-chain by [ExpressExchangeEntity.payinHash]); incoming views the `to` side (payout, joined by
 * [ExpressExchangeEntity.payoutHash]).
 */
internal class ExpressSwapConverter : Converter<ExpressSwapConverter.Input, ExpressTx.Swap> {

    override fun convert(value: Input): ExpressTx.Swap = ExpressTx.Swap(
        tx = convertExchangeTransaction(value.entity, value.provider),
        isOutgoing = value.isOutgoing,
        txInfo = null,
    )

    data class Input(
        val entity: ExpressExchangeEntity,
        val provider: ExpressProvider?,
        val isOutgoing: Boolean,
    )
}

internal class ExpressOnrampConverter : Converter<ExpressOnrampConverter.Input, ExpressTx.Onramp> {

    override fun convert(value: Input): ExpressTx.Onramp {
        val entity = value.entity
        return ExpressTx.Onramp(
            tx = OnrampTransaction(
                txId = entity.txId,
                status = ExpressOnrampStatus.fromRaw(entity.status),
                createdAtMillis = parseIsoMillis(entity.createdAt),
                provider = value.provider,
                payoutHash = entity.payoutHash,
                fromFiat = Amount(
                    currencySymbol = entity.fromCurrencyCode,
                    value = entity.fromAmount.toBigDecimalOrZero(),
                    decimals = entity.fromPrecision,
                    type = AmountType.FiatType(code = entity.fromCurrencyCode),
                ),
                toAsset = ExpressTransactionAsset(
                    id = ExpressAssetId(networkId = entity.to.network, contractAddress = entity.to.contractAddress),
                    amount = (entity.to.actualAmount ?: entity.to.amount).toBigDecimalOrZero(),
                    decimals = entity.to.decimals,
                ),
            ),
            txInfo = null,
        )
    }

    data class Input(val entity: ExpressOnrampEntity, val provider: ExpressProvider?)
}

private fun convertExchangeTransaction(entity: ExpressExchangeEntity, provider: ExpressProvider?): ExchangeTransaction {
    return ExchangeTransaction(
        txId = entity.txId,
        status = ExpressExchangeStatus.fromRaw(entity.status),
        createdAtMillis = parseIsoMillis(entity.createdAt),
        provider = provider,
        payinHash = entity.payinHash,
        payoutHash = entity.payoutHash,
        fromAsset = ExpressTransactionAsset(
            id = ExpressAssetId(networkId = entity.from.network, contractAddress = entity.from.contractAddress),
            amount = entity.from.amount.toBigDecimalOrZero(),
            decimals = entity.from.decimals,
        ),
        toAsset = ExpressTransactionAsset(
            id = ExpressAssetId(networkId = entity.to.network, contractAddress = entity.to.contractAddress),
            amount = (entity.to.actualAmount ?: entity.to.amount).toBigDecimalOrZero(),
            decimals = entity.to.decimals,
        ),
    )
}

private fun parseIsoMillis(iso: String): Long = DateTime.parse(iso).millis

private fun String?.toBigDecimalOrZero(): BigDecimal = this?.toBigDecimalOrNull() ?: BigDecimal.ZERO

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