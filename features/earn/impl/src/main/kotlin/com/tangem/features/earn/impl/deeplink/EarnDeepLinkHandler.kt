package com.tangem.features.earn.impl.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.EARN_PRODUCT_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.navigation.pushIfAbsent
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.impl.overview.EarnAvailability
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class EarnDeepLinkHandler @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val earnAvailability: EarnAvailability,
    private val productsRegistry: EarnProductsRegistry,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val router = request.router
        val queryParams = request.queryParams
        val tokenId = queryParams[TOKEN_ID_KEY]?.takeIf(String::isNotBlank)
        val networkId = queryParams[NETWORK_ID_KEY]?.takeIf(String::isNotBlank)

        if (tokenId == null || networkId == null) {
            openMain(router, reason = "missing token_id or network_id")
            return
        }

        val userWalletId = userWalletsListRepository.selectedUserWallet.value?.walletId
        if (userWalletId == null) {
            openMain(router, reason = "no selected user wallet")
            return
        }

        request.scope.launch {
            val accountList = singleAccountListSupplier.getSyncOrNull(userWalletId)
            val isRequested = { currency: CryptoCurrency ->
                currency.network.rawId.equals(networkId, ignoreCase = true) &&
                    currency.id.rawCurrencyId?.value.equals(tokenId, ignoreCase = true)
            }
            val currency = accountList?.mainAccount?.cryptoCurrencies?.firstOrNull(isRequested)
                ?: accountList?.flattenCurrencies()?.firstOrNull(isRequested)

            when {
                currency == null -> openMain(router, reason = "token $tokenId in $networkId is not in the wallet")
                !earnAvailability.isEarnAvailable(userWalletId, currency) -> {
                    openMain(router, reason = "Earn is not available for ${currency.name}")
                }
                else -> router.pushIfAbsent(
                    AppRoute.EarnEntry(
                        userWalletId = userWalletId,
                        cryptoCurrency = currency,
                        source = AppRoute.EarnEntry.Source.Deeplink,
                        start = resolveStart(queryParams[EARN_PRODUCT_KEY]),
                    ),
                )
            }
        }
    }

    private fun resolveStart(product: String?): AppRoute.EarnEntry.Start {
        val productId = product?.lowercase()?.let(::EarnProductId)
            ?.takeIf { productsRegistry.find(it) != null }
            ?: return AppRoute.EarnEntry.Start.Auto

        return AppRoute.EarnEntry.Start.Info(productId)
    }

    private fun openMain(router: Router, reason: String) {
        TangemLogger.i("Earn deeplink: $reason, opening the main screen")
        router.popTo(AppRoute.Wallet)
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://earn"
    }
}