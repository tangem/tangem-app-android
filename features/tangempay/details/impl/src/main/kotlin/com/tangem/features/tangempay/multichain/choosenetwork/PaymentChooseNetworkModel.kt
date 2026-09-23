package com.tangem.features.tangempay.multichain.choosenetwork

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.CreatePaymentNetworkContractUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.common.networksOrNull
import com.tangem.features.tangempay.multichain.isOtherWay
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Stable
@ModelScoped
internal class PaymentChooseNetworkModel @Inject constructor(
    paramsContainer: ParamsContainer,
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val createPaymentNetworkContractUseCase: CreatePaymentNetworkContractUseCase,
    private val analytics: AnalyticsEventHandler,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PaymentChooseNetworkComponent.Params>()

    private val converter = PaymentChooseNetworkUMConverter(
        onNetworkClick = ::onNetworkClick,
        onDismiss = ::onDismiss,
    )

    /**
     * Local Loading/Error overrides layered on top of the network-derived rows, keyed by row id.
     * Needed because a `NOT_ISSUED` -> contract-creation attempt does not, by itself, produce a fresh
     * [paymentAccountStatusSupplier] emission (only a *completed* order does, via the poller's status
     * refresh) — so Loading/Error must be tracked here rather than derived purely from network state.
     */
    private val rowOverrides = MutableStateFlow<Map<String, RowOverride>>(emptyMap())

    val uiState: StateFlow<PaymentChooseNetworkUM>
        field = MutableStateFlow(converter.convert(emptyList()))

    init {
        analytics.send(TangemPayAnalyticsEvents.Multichain.ChooseNetworkPopupShowed())

        val accountStatuses = paymentAccountStatusSupplier.invoke(params.walletId)

        accountStatuses
            .onEach { status ->
                val networks = status.networksOrNull() ?: return@onEach
                onNetworksBecameAvailable(networks)
            }
            .launchIn(modelScope)

        combine(accountStatuses, rowOverrides) { status, overrides ->
            val networks = status.networksOrNull() ?: return@combine null
            applyOverrides(converter.convert(networks), overrides)
        }
            .onEach { converted -> converted?.let { uiState.value = it } }
            .launchIn(modelScope)
    }

    fun onDismiss() {
        params.listener.onDismiss()
    }

    private fun onNetworkClick(status: PaymentNetworkStatus) {
        analytics.send(clickEvent(status))
        when (status) {
            is PaymentNetworkStatus.Available -> params.listener.onSelectAvailable(
                networkRawId = status.network.rawId,
            )
            is PaymentNetworkStatus.NotIssued -> onNotIssuedClick(status)
            is PaymentNetworkStatus.Disabled -> params.listener.onSelectDisabled()
        }
    }

    private fun clickEvent(status: PaymentNetworkStatus): TangemPayAnalyticsEvents = if (status.isOtherWay()) {
        TangemPayAnalyticsEvents.Multichain.OtherWayNetworkClicked(
            blockchain = status.network.name,
            chainId = status.chainId,
        )
    } else {
        TangemPayAnalyticsEvents.Multichain.FastWayNetworkClicked(
            blockchain = status.network.name,
            chainId = status.chainId,
        )
    }

    /**
     * Starts the on-demand network-contract-creation order for a tapped `NOT_ISSUED` row, driving that
     * row's own Loading/Error state (see [rowOverrides]). The use case is fire-and-forget: it returns
     * once the order is active, while completion polling runs in the app scope and survives this model.
     * The success signal is the network flipping to `Available` in a status emission — handled by
     * [onNetworksBecameAvailable], which also opens the Receive sheet. If that flip never arrives
     * (poll timeout, backend lag), [PENDING_TIMEOUT] stops the spinner and offers retry.
     */
    private fun onNotIssuedClick(status: PaymentNetworkStatus.NotIssued) {
        val rowId = status.network.rawId
        // Reentrancy guard — a fast double-tap must not fire two concurrent create-order attempts.
        if (rowOverrides.value[rowId]?.state == PaymentNetworkItemUM.State.Loading) return

        rowOverrides.update { it + (rowId to RowOverride(state = PaymentNetworkItemUM.State.Loading, onRetry = null)) }
        modelScope.launch {
            createPaymentNetworkContractUseCase(params.walletId, status.network).fold(
                ifLeft = { markFailed(rowId, status) },
                ifRight = {
                    delay(PENDING_TIMEOUT)
                    if (rowOverrides.value[rowId]?.state == PaymentNetworkItemUM.State.Loading) {
                        markFailed(rowId, status)
                    }
                },
            )
        }
    }

    /**
     * Clears a row's local override once its network is issued (`Available`) — an override must never
     * shadow live Available data. A row that was still Loading got there from a contract-creation tap;
     * since polling is fire-and-forget, this transition IS the success signal, so honor the user's
     * intent and open the Receive sheet right away (the row id doubles as the network's stable rawId).
     */
    private fun onNetworksBecameAvailable(networks: List<PaymentNetworkStatus>) {
        val availableRawIds = networks
            .filterIsInstance<PaymentNetworkStatus.Available>()
            .map { it.network.rawId }
            .toSet()
        rowOverrides.value.forEach { (rowId, override) ->
            if (rowId in availableRawIds) {
                clearOverride(rowId)
                if (override.state == PaymentNetworkItemUM.State.Loading) {
                    params.listener.onSelectAvailable(networkRawId = rowId)
                }
            }
        }
    }

    private fun markFailed(rowId: String, status: PaymentNetworkStatus.NotIssued) {
        analytics.send(
            TangemPayAnalyticsEvents.Multichain.AddressFetchErrorShowed(
                blockchain = status.network.name,
                chainId = status.chainId,
            ),
        )
        val override = RowOverride(
            state = PaymentNetworkItemUM.State.Error,
            onRetry = { onNotIssuedClick(status) },
        )
        rowOverrides.update { it + (rowId to override) }
    }

    private fun clearOverride(rowId: String) {
        rowOverrides.update { it - rowId }
    }

    private fun applyOverrides(
        um: PaymentChooseNetworkUM,
        overrides: Map<String, RowOverride>,
    ): PaymentChooseNetworkUM {
        if (overrides.isEmpty()) return um
        fun apply(item: PaymentNetworkItemUM): PaymentNetworkItemUM {
            val override = overrides[item.id] ?: return item
            return item.copy(state = override.state, onRetry = override.onRetry)
        }
        return um.copy(
            fastWay = um.fastWay.map(::apply).toPersistentList(),
            otherWays = um.otherWays.map(::apply).toPersistentList(),
        )
    }

    private data class RowOverride(val state: PaymentNetworkItemUM.State, val onRetry: (() -> Unit)?)

    private companion object {
        /**
         * The background poll gives up after 60s; with a margin for the final status refresh to land,
         * a row still Loading after this window is considered failed (retry stays available — the
         * order itself may still complete later, in which case the row simply flips to Available).
         */
        val PENDING_TIMEOUT = 90.seconds
    }
}