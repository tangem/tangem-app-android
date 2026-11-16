package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.navigation.pushIfAbsent
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetAvailabilityUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class YieldDeepLinkHandler @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val yieldSupplyGetAvailabilityUseCase: YieldSupplyGetAvailabilityUseCase,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val router = request.router
        val queryParams = request.queryParams
        val tokenId = queryParams[TOKEN_ID_KEY]?.takeIf { it.isNotBlank() }
        val networkId = queryParams[NETWORK_ID_KEY]?.takeIf { it.isNotBlank() }

        if (tokenId == null || networkId == null) {
            TangemLogger.i("Yield deeplink: missing token_id or network_id; falling back to earn yield list")
            pushFallback(router, networkId)
            return
        }

        request.scope.launch {
            val userWallet = getSelectedWalletSyncUseCase().getOrNull()
            if (userWallet == null) {
                TangemLogger.e("Yield deeplink: no selected user wallet")
                pushFallback(router, networkId)
                return@launch
            }

            val cryptoCurrency = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWallet.walletId),
            )
                .orEmpty()
                .firstOrNull { currency ->
                    currency.network.rawId.equals(networkId, ignoreCase = true) &&
                        currency.id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true
                }

            if (cryptoCurrency == null) {
                TangemLogger.i(
                    """
                        Yield deeplink: token not in selected wallet
                        |- $TOKEN_ID_KEY: $tokenId
                        |- $NETWORK_ID_KEY: $networkId
                    """.trimIndent(),
                )
                pushFallback(router, networkId)
                return@launch
            }

            val availability = yieldSupplyGetAvailabilityUseCase(cryptoCurrency).getOrNull()
            val available = availability as? YieldSupplyAvailability.Available
            if (available == null) {
                TangemLogger.i("Yield deeplink: yield not available for ${cryptoCurrency.name}")
                pushFallback(router, networkId)
                return@launch
            }

            router.pushIfAbsent(
                AppRoute.YieldSupplyEntry(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = cryptoCurrency,
                    apy = available.apy,
                ),
            )
        }
    }

    private fun pushFallback(router: Router, networkId: String?) {
        router.pushIfAbsent(
            AppRoute.Earn(
                preselectedEarnType = PreselectedEarnType.Yield,
                preselectedNetworkId = networkId,
            ),
        )
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://yield"
    }
}