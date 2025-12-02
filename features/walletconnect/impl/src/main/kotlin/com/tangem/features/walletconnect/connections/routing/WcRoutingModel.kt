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
import timber.log.Timber
import javax.inject.Inject

private const val WC_TAG = "Wallet Connect"

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

    init {
        setupQueue()
    }

    fun onSlotEmpty() {
        Timber.d("WC Queue: onSlotEmpty() called")
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
                Timber.d("WC Queue: Received configuration $configuration, waiting for queue ready")
                awaitQueueReady()
                Timber.d("WC Queue: Queue ready, pushing configuration")
                isSlotEmpty.update { false }
                innerRouter.push(configuration)
            }
            .launchIn(modelScope)
    }

    private suspend fun awaitQueueReady() = combine(
        isSlotEmpty,
        permittedAppRoute,
        cardSdkProvider.sdk.uiVisibility(),
    ) { isSlotEmpty, permittedAppRoute, isCardSdkVisible ->
        val ready = isSlotEmpty && permittedAppRoute && !isCardSdkVisible
        Timber.d("WC Queue: isSlotEmpty=$isSlotEmpty, permittedAppRoute=$permittedAppRoute, isCardSdkVisible=$isCardSdkVisible, ready=$ready")
        ready
    }.first { it }

    fun onAppRouteChange(appRoute: AppRoute) {
        permittedAppRoute.value = when (appRoute) {
            AppRoute.Initial,
            is AppRoute.Welcome,
            is AppRoute.Disclaimer,
            is AppRoute.Stories,
            -> {
                innerRouter.pop()
                false
            }
            else -> true
        }
    }
}