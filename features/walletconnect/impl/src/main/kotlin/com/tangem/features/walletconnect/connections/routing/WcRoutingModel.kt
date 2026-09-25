package com.tangem.features.walletconnect.connections.routing

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.WcRequestService
import com.tangem.domain.walletconnect.model.WcBitcoinMethodName
import com.tangem.domain.walletconnect.model.WcEthMethodName
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.WcSolanaMethodName
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

@ModelScoped
internal class WcRoutingModel @Inject constructor(
    private val requestService: WcRequestService,
    private val pairService: WcPairService,
    private val cardSdkProvider: CardSdkProvider,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val innerRouter = WcRouter(SlotNavigation())

    private val isSlotEmpty = MutableStateFlow(true)
    private val permittedAppRoute = MutableStateFlow(false)

    /** The configuration currently shown in the slot, `null` when the slot is empty. */
    private var activeRoute: WcInnerRoute? = null

    init {
        setupQueue()
    }

    fun onSlotEmpty() {
        TangemLogger.d("WC Queue: onSlotEmpty() called")
        activeRoute = null
        isSlotEmpty.update { true }
    }

    private fun setupQueue() {
        val requestFlow = requestService.wcRequest
            .map { (methodName, rawRequest) ->
                when (methodName) {
                    WcEthMethodName.EthSign,
                    WcEthMethodName.PersonalSign,
                    WcEthMethodName.SignTypeData,
                    WcEthMethodName.SignTypeDataV4,
                    WcSolanaMethodName.SignMessage,
                    WcBitcoinMethodName.SignMessage,
                    -> {
                        WcInnerRoute.SignMessage(rawRequest)
                    }
                    WcEthMethodName.AddEthereumChain,
                    -> {
                        WcInnerRoute.AddNetwork(rawRequest)
                    }
                    WcEthMethodName.SwitchEthereumChain,
                    -> {
                        WcInnerRoute.SwitchNetwork(rawRequest)
                    }
                    WcEthMethodName.SignTransaction,
                    WcEthMethodName.SendTransaction,
                    WcSolanaMethodName.SignTransaction,
                    WcSolanaMethodName.SignAndSendTransaction,
                    WcSolanaMethodName.SendAllTransaction,
                    WcBitcoinMethodName.SendTransfer,
                    WcBitcoinMethodName.SignPsbt,
                    -> {
                        WcInnerRoute.Send(rawRequest)
                    }
                    WcBitcoinMethodName.GetAccountAddresses,
                    -> {
                        WcInnerRoute.GetAddresses(rawRequest)
                    }
                    is WcMethodName.Unsupported,
                    -> {
                        WcInnerRoute.UnsupportedMethodAlert
                    }
                }
            }

        val pairFlow = pairService.pairFlow
            .map { request -> WcInnerRoute.Pair(request) }

        merge(requestFlow, pairFlow)
            .onEach { configuration ->
                TangemLogger.d("WC Queue: Received configuration $configuration, waiting for queue ready")
                awaitQueueReady()
                TangemLogger.d("WC Queue: Queue ready, pushing configuration")
                isSlotEmpty.update { false }
                activeRoute = configuration
                innerRouter.push(configuration)
            }
            .launchIn(modelScope)
    }

    private suspend fun awaitQueueReady() = combine(
        isSlotEmpty,
        permittedAppRoute,
        cardSdkProvider.sdk.uiVisibility(),
    ) { isSlotEmpty, permittedAppRoute, isCardSdkVisible ->
        val isReady = isSlotEmpty && permittedAppRoute && !isCardSdkVisible
        TangemLogger.d(
            "WC Queue: isSlotEmpty=$isSlotEmpty, permittedAppRoute=$permittedAppRoute, " +
                "isCardSdkVisible=$isCardSdkVisible, ready=$isReady",
        )
        isReady
    }.first { it }

    fun onAppRouteChange(appRoute: AppRoute) {
        permittedAppRoute.value = when (appRoute) {
            AppRoute.Initial,
            is AppRoute.Welcome,
            is AppRoute.Disclaimer,
            is AppRoute.Stories,
            -> {
                // The slot child is destroyed here before it can answer the dApp itself (its cancel() goes
                // through a channel collected in the model scope that is being cancelled), so the pending
                // request would only end with a timeout on the dApp side. Reject it explicitly.
                (activeRoute as? WcInnerRoute.Method)?.let { method ->
                    TangemLogger.d("WC Queue: rejecting request ${method.rawRequest.request.id} on forced pop")
                    requestService.rejectNonBlock(method.rawRequest)
                }
                innerRouter.pop()
                false
            }
            else -> true
        }
    }
}