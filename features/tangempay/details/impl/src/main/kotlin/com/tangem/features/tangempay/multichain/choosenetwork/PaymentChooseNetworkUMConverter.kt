package com.tangem.features.tangempay.multichain.choosenetwork

import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.features.tangempay.multichain.toRowData
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

/**
 * Maps the multichain networks of a payment account into the Choose-network bottom sheet sections:
 * **Fast way** ([PaymentNetworkStatus.Available] + [PaymentNetworkStatus.NotIssued], input order preserved) and
 * **Other ways** ([PaymentNetworkStatus.Disabled]). Wires each row's click to the matching [listener] callback,
 * except [PaymentNetworkStatus.NotIssued] rows, which are routed to [onSelectNotIssued] instead — contract
 * creation is owned by [com.tangem.features.tangempay.multichain.choosenetwork.PaymentChooseNetworkModel] itself, since it needs
 * to drive that row's own Loading/Error state.
 */
internal class PaymentChooseNetworkUMConverter(
    private val listener: ChooseNetworkListener,
    private val onSelectNotIssued: (PaymentNetworkStatus.NotIssued) -> Unit,
) : Converter<List<PaymentNetworkStatus>, PaymentChooseNetworkUM> {

    override fun convert(value: List<PaymentNetworkStatus>): PaymentChooseNetworkUM {
        return PaymentChooseNetworkUM(
            fastWay = value
                .filter { it !is PaymentNetworkStatus.Disabled }
                .mapNotNull(::toItemUM)
                .toPersistentList(),
            otherWays = value
                .filterIsInstance<PaymentNetworkStatus.Disabled>()
                .mapNotNull(::toItemUM)
                .toPersistentList(),
            dismiss = listener::onDismiss,
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

    private fun onNetworkClick(status: PaymentNetworkStatus) {
        when (status) {
            is PaymentNetworkStatus.Available -> listener.onSelectAvailable(networkRawId = status.network.rawId)
            is PaymentNetworkStatus.NotIssued -> onSelectNotIssued(status)
            is PaymentNetworkStatus.Disabled -> listener.onSelectDisabled()
        }
    }
}