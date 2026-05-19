package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetAvailabilityUseCase
import com.tangem.features.feed.entry.deeplink.YieldDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DefaultYieldDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val yieldSupplyGetAvailabilityUseCase: YieldSupplyGetAvailabilityUseCase,
) : YieldDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val tokenId = queryParams[TOKEN_ID_KEY]?.takeIf { it.isNotBlank() }
        val networkId = queryParams[NETWORK_ID_KEY]?.takeIf { it.isNotBlank() }

        if (tokenId == null || networkId == null) {
            TangemLogger.i("Yield deeplink: missing token_id or network_id; falling back to earn yield list")
            pushFallback(networkId)
            return
        }

        scope.launch {
            val userWallet = getSelectedWalletSyncUseCase().getOrNull()
            if (userWallet == null) {
                TangemLogger.e("Yield deeplink: no selected user wallet")
                pushFallback(networkId)
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
                pushFallback(networkId)
                return@launch
            }

            val availability = yieldSupplyGetAvailabilityUseCase(cryptoCurrency).getOrNull()
            val available = availability as? YieldSupplyAvailability.Available
            if (available == null) {
                TangemLogger.i("Yield deeplink: yield not available for ${cryptoCurrency.name}")
                pushFallback(networkId)
                return@launch
            }

            appRouter.push(
                AppRoute.YieldSupplyEntry(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = cryptoCurrency,
                    apy = available.apy,
                ),
            )
        }
    }

    private fun pushFallback(networkId: String?) {
        appRouter.push(
            AppRoute.Earn(
                preselectedEarnType = PreselectedEarnType.Yield,
                preselectedNetworkId = networkId,
            ),
        )
    }

    @AssistedFactory
    interface Factory : YieldDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultYieldDeepLinkHandler
    }
}