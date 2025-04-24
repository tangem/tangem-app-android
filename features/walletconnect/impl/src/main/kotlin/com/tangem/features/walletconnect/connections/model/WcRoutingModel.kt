package com.tangem.features.walletconnect.connections.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.WcRequestService
import com.tangem.domain.walletconnect.model.WcEthMethodName
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.WcSolanaMethodName
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.features.walletconnect.components.WalletConnectFeatureToggles
import com.tangem.features.walletconnect.connections.routes.WcInnerRoute
import com.tangem.features.walletconnect.connections.routes.WcInnerRoute.SignMessage
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class WcRoutingModel @Inject constructor(
    private val wcUseCase: WcInitializeUseCase,
    private val requestService: WcRequestService,
    private val pairService: WcPairService,
    override val dispatchers: CoroutineDispatcherProvider,
    private val featureToggles: WalletConnectFeatureToggles,
) : Model() {

    val navigation: SlotNavigation<WcInnerRoute> = SlotNavigation()

    private val isSlotEmpty = MutableStateFlow(true)
    private val permittedAppRoute = MutableStateFlow(false)

    init {
        modelScope.launch {
            awaitReady()
            if (!featureToggles.isRedesignedWalletConnectEnabled) return@launch
            wcUseCase.init()
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
                    -> SignMessage(rawRequest)
                    WcEthMethodName.SignTypeData -> TODO()
                    WcEthMethodName.SignTypeDataV4 -> TODO()
                    WcEthMethodName.SignTransaction -> TODO()
                    WcEthMethodName.SendTransaction -> TODO()
                    WcSolanaMethodName.SignMessage -> TODO()
                    WcSolanaMethodName.SignTransaction -> TODO()
                    WcSolanaMethodName.SendAllTransaction -> TODO()
                    is WcMethodName.Unsupported -> TODO()
                }
            }

        val pairFlow = pairService.pairFlow
            .map { request -> WcInnerRoute.Pair(request) }

        merge(requestFlow, pairFlow)
            .onEach { configuration ->
                awaitReady()
                isSlotEmpty.update { false }
                navigation.activate(configuration)
            }
            .launchIn(modelScope)
    }

    private suspend fun awaitReady() = combine(
        isSlotEmpty,
        permittedAppRoute,
    ) { isSlotEmpty, permittedAppRoute -> isSlotEmpty && permittedAppRoute }
        .first { it }

    fun onAppRouteChange(appRoute: AppRoute) {
        permittedAppRoute.value = when (appRoute) {
            AppRoute.Initial,
            is AppRoute.Welcome,
            is AppRoute.Disclaimer,
            is AppRoute.Stories,
            -> false
            else -> true
        }
    }
}