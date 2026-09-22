package com.tangem.features.tangempay.multichain.choosenetwork

import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.features.tangempay.multichain.isOtherWay
import com.tangem.features.tangempay.multichain.toRowData
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

/**
 * Maps the multichain networks of a payment account into the Choose-network bottom sheet sections:
 * **Fast way** ([PaymentNetworkStatus.Available] + [PaymentNetworkStatus.NotIssued], input order preserved) and
 * **Other ways** ([PaymentNetworkStatus.Disabled]), see [isOtherWay]. Every row tap is handed back as the
 * tapped status via [onNetworkClick]; routing it — and reporting it to analytics — is owned by
 * [PaymentChooseNetworkModel], which alone knows the destinations and holds the per-row Loading/Error state.
 */
internal class PaymentChooseNetworkUMConverter(
    private val onNetworkClick: (PaymentNetworkStatus) -> Unit,
    private val onDismiss: () -> Unit,
) : Converter<List<PaymentNetworkStatus>, PaymentChooseNetworkUM> {

    override fun convert(value: List<PaymentNetworkStatus>): PaymentChooseNetworkUM {
        val (otherWays, fastWay) = value.partition(PaymentNetworkStatus::isOtherWay)
        return PaymentChooseNetworkUM(
            fastWay = fastWay.mapNotNull(::toItemUM).toPersistentList(),
            otherWays = otherWays.mapNotNull(::toItemUM).toPersistentList(),
            dismiss = onDismiss,
        )
    }

    private fun toItemUM(status: PaymentNetworkStatus): PaymentNetworkItemUM? {
        val row = status.toRowData() ?: return null
        return PaymentNetworkItemUM(
            id = row.id,
            name = row.name,
            tokensLabel = row.tokensLabel,
            iconResId = row.iconResId,
            state = PaymentNetworkItemUM.State.Idle,
            onClick = { onNetworkClick(status) },
        )
    }
}