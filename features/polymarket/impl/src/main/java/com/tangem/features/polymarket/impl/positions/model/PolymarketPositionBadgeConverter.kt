package com.tangem.features.polymarket.impl.positions.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_split_top_16
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_16
import com.tangem.domain.polymarket.converter.PolymarketPositionStatusConverter
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionStatus
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class PolymarketPositionBadgeConverter(private val shouldShowAmount: Boolean = true) :
    Converter<PolymarketPosition, TangemBadgeUM?> {

    override fun convert(value: PolymarketPosition): TangemBadgeUM? = when (value) {
        is PolymarketPosition.Open -> openBadge(value)
        is PolymarketPosition.Closed -> closedBadge(value)
    }

    private fun openBadge(value: PolymarketPosition.Open): TangemBadgeUM? =
        when (PolymarketPositionStatusConverter.convert(value)) {
            PolymarketPositionStatus.WON -> badge(
                label = R.string.prediction_positions_won,
                status = TangemBadge.Status.Success,
                amount = value.currentValue,
                icon = Icons.ic_checkmark_16,
                variant = TangemBadge.Variant.Solid,
            )
            PolymarketPositionStatus.LOST -> badge(
                label = R.string.prediction_positions_lost,
                status = TangemBadge.Status.Error,
                amount = value.currentValue,
                icon = Icons.ic_heart_broken_16,
            )
            PolymarketPositionStatus.SPLIT -> badge(
                label = R.string.prediction_positions_split,
                amount = value.currentValue,
                icon = Icons.ic_arrow_split_top_16,
            )
            PolymarketPositionStatus.PENDING ->
                badge(label = R.string.prediction_positions_pending, status = TangemBadge.Status.Warning)
            PolymarketPositionStatus.DISPUTED ->
                badge(label = R.string.prediction_positions_disputed, status = TangemBadge.Status.Warning)
            PolymarketPositionStatus.UNKNOWN ->
                if (value.result != null) badge(label = R.string.prediction_positions_unknown) else null
            else -> null
        }

    private fun closedBadge(value: PolymarketPosition.Closed): TangemBadgeUM =
        when (PolymarketPositionStatusConverter.convert(value)) {
            PolymarketPositionStatus.WON -> badge(
                label = R.string.prediction_positions_won,
                status = TangemBadge.Status.Success,
                amount = value.realizedPnl,
                icon = Icons.ic_checkmark_16,
            )
            PolymarketPositionStatus.LOST -> badge(
                label = R.string.prediction_positions_lost,
                status = TangemBadge.Status.Error,
                amount = value.realizedPnl,
                icon = Icons.ic_heart_broken_16,
            )
            PolymarketPositionStatus.SPLIT -> badge(
                label = R.string.prediction_positions_split,
                amount = value.realizedPnl,
                icon = Icons.ic_arrow_split_top_16,
            )
            PolymarketPositionStatus.CLOSED -> badge(
                label = R.string.prediction_positions_closed,
                status = when (value.realizedPnl.signum()) {
                    1 -> TangemBadge.Status.Success
                    -1 -> TangemBadge.Status.Error
                    else -> TangemBadge.Status.Neutral
                },
                amount = value.realizedPnl,
            )
            else -> badge(
                label = R.string.prediction_positions_unknown,
                amount = value.realizedPnl,
            )
        }

    private fun badge(
        label: Int,
        status: TangemBadge.Status = TangemBadge.Status.Neutral,
        amount: BigDecimal? = null,
        icon: ImageVector? = null,
        variant: TangemBadge.Variant = TangemBadge.Variant.Tinted,
    ) = TangemBadgeUM(
        text = if (shouldShowAmount && amount != null) {
            resourceReference(
                R.string.prediction_positions_result_amount,
                wrappedList(resourceReference(label), amount.abs().formatPolymarketMoney()),
            )
        } else {
            resourceReference(label)
        },
        variant = variant,
        status = status,
        size = TangemBadge.Size.X6,
        iconStart = icon?.let { TangemIconUM.Icon(imageVector = it) },
    )
}