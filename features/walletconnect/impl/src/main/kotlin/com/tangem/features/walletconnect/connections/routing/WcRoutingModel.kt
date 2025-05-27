package com.tangem.features.walletconnect.connections.routing

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.WcRequestService
import com.tangem.domain.walletconnect.model.WcEthMethodName
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.WcSolanaMethodName
import com.tangem.features.walletconnect.components.WalletConnectFeatureToggles
import com.tangem.features.walletconnect.connections.components.AlertsComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class WcRoutingModel @Inject constructor(
    private val requestService: WcRequestService,
    private val pairService: WcPairService,
    private val cardSdkProvider: CardSdkProvider,
    override val dispatchers: CoroutineDispatcherProvider,
    private val featureToggles: WalletConnectFeatureToggles,
) : Model() {

    val innerRouter = WcRouter(SlotNavigation<WcInnerRoute>())

    private val isSlotEmpty = MutableStateFlow(true)
    private val permittedAppRoute = MutableStateFlow(false)

    init {
        if (featureToggles.isRedesignedWalletConnectEnabled) {
            setupQueue()
        }
    }

    fun onSlotEmpty() {
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
                    -> WcInnerRoute.SignMessage(rawRequest)
                    WcEthMethodName.AddEthereumChain,
                    -> WcInnerRoute.AddNetwork(rawRequest)
                    WcEthMethodName.SignTransaction,
                    WcEthMethodName.SendTransaction,
                    WcSolanaMethodName.SignTransaction,
                    WcSolanaMethodName.SendAllTransaction,
                    is WcMethodName.Unsupported,
                    -> WcInnerRoute.Alert(
                        alertType = AlertsComponent.AlertType.UnsupportedMethod { innerRouter.pop() },
                    )
                }
            }

        val pairFlow = pairService.pairFlow
            .map { request -> WcInnerRoute.Pair(request) }

        merge(requestFlow, pairFlow)
            .onEach { configuration ->
                awaitQueueReady()
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
        isSlotEmpty && permittedAppRoute && !isCardSdkVisible
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