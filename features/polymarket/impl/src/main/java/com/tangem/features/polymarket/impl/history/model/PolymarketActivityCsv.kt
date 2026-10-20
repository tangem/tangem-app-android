package com.tangem.features.polymarket.impl.history.model

import com.tangem.domain.polymarket.model.PolymarketActivity
import java.math.BigDecimal
import java.time.temporal.ChronoUnit

internal object PolymarketActivityCsv {

    private val outgoingTypes = setOf(PolymarketActivity.Type.BUY, PolymarketActivity.Type.WITHDRAW)
    private val formulaPrefixes = setOf('=', '+', '-', '@', '\t', '\r', '\n')

    fun make(records: List<PolymarketActivity>, headers: List<String>): String = buildString {
        append(headers.joinToString(",", transform = ::text))
        records.forEach { record ->
            append("\r\n")
            append(line(record).joinToString(","))
        }
    }

    private fun line(record: PolymarketActivity): List<String> {
        val market = record.market
        val order = record.order
        val amount = if (record.type in outgoingTypes && record.amount.signum() > 0) {
            record.amount.negate()
        } else {
            record.amount
        }
        return listOf(
            quoted(record.occurredAt.truncatedTo(ChronoUnit.SECONDS).toString()),
            quoted(record.type.name),
            quoted(record.redeemResult?.name),
            number(amount),
            text(market?.title),
            text(market?.event?.title),
            text(market?.outcome),
            number(market?.shares),
            number(order?.averagePrice),
            number(order?.platformFee),
            number(order?.builderFee),
            number(order?.notional),
            quoted(record.transactionHash),
        )
    }

    private fun number(value: BigDecimal?): String = quoted(value?.stripTrailingZeros()?.toPlainString())

    private fun text(value: String?): String = quoted(
        if (value?.firstOrNull() in formulaPrefixes) "'$value" else value,
    )

    private fun quoted(value: String?): String = "\"${value.orEmpty().replace("\"", "\"\"")}\""
}