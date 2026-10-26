package com.tangem.features.polymarket.impl.history.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM.Type
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.DateGroup
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.PLUS
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PolymarketActivityUMConverter(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG),
) {

    fun convert(items: List<PolymarketActivity>): ImmutableList<DateGroup> {
        val today = LocalDate.now(clock)
        return items.sortedByDescending { it.occurredAt }
            .groupBy { it.occurredAt.atZone(clock.zone).toLocalDate() }
            .map { (date, activities) ->
                DateGroup(
                    id = date.toString(),
                    title = if (date == today) {
                        resourceReference(R.string.common_today)
                    } else {
                        stringReference(dateFormatter.format(date))
                    },
                    activities = activities.map(::convertRow).toImmutableList(),
                )
            }.toImmutableList()
    }

    private fun convertRow(activity: PolymarketActivity): PolymarketActivityRowUM {
        val type = activity.rowType()
        val money = activity.amount.abs().formatPolymarketMoney()
        return PolymarketActivityRowUM(
            id = activity.id,
            type = type,
            title = type.title(),
            subtitle = PolymarketActivitySubtitleUM.Plain(
                stringReference(
                    activity.market?.title ?: activity.market?.event?.title ?: activity.transactionHash.orEmpty(),
                ),
            ),
            amount = stringReference(
                when (type) {
                    Type.LOST -> MINUS
                    Type.BOUGHT, Type.WITHDRAWN -> "$MINUS $money"
                    Type.SOLD, Type.FUNDS_ADDED, Type.REDEEMED -> "$PLUS $money"
                },
            ),
            iconUrl = activity.market?.icon ?: activity.market?.image
                ?: activity.market?.event?.icon ?: activity.market?.event?.image,
        )
    }

    private fun PolymarketActivity.rowType(): Type = when (type) {
        PolymarketActivity.Type.BUY -> Type.BOUGHT
        PolymarketActivity.Type.SELL -> Type.SOLD
        PolymarketActivity.Type.DEPOSIT -> Type.FUNDS_ADDED
        PolymarketActivity.Type.WITHDRAW -> Type.WITHDRAWN
        PolymarketActivity.Type.REDEEM -> if (redeemResult == PolymarketActivity.RedeemResult.LOST) {
            Type.LOST
        } else {
            Type.REDEEMED
        }
    }

    private fun Type.title() = resourceReference(
        when (this) {
            Type.BOUGHT -> R.string.prediction_history_bought
            Type.SOLD -> R.string.prediction_history_sold
            Type.FUNDS_ADDED -> R.string.prediction_history_funds_added
            Type.WITHDRAWN -> R.string.transaction_history_withdrawn
            Type.REDEEMED -> R.string.prediction_history_redeemed
            Type.LOST -> R.string.prediction_positions_lost
        },
    )
}