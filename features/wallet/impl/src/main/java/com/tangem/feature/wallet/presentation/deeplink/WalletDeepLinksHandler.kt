package com.tangem.feature.wallet.presentation.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.deeplink.DeepLink
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.global.SellCurrencyDeepLink
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.launchOnCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class WalletDeepLinksHandler @Inject constructor(
    private val deepLinksRegistry: DeepLinksRegistry,
    private val getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    private val router: AppRouter,
) {

    private var deepLinksMap = mutableMapOf<UserWalletId, List<DeepLink>>()

    fun registerForWallet(scope: CoroutineScope, userWallet: UserWallet) {
        val deepLinks = deepLinksMap.getOrPut(userWallet.walletId) {
            getDeepLinks(userWallet, scope)
        }
        deepLinksRegistry.unregisterByIds(deepLinks.map { it.id })
        deepLinksRegistry.register(deepLinks = deepLinks)

        // When navigation to another screen scope is Cancelled and deeplinks are hot handled
        scope.launchOnCancellation {
            deepLinksRegistry.unregister(deepLinks)
        }
    }

    private fun getDeepLinks(userWallet: UserWallet, scope: CoroutineScope): List<DeepLink> {
        val sellCurrencyDeepLink = SellCurrencyDeepLink(
            onReceive = { data ->
                scope.launch {
                    onSellCurrencyDeepLink(userWallet, data)
                }
            },
            shouldHandleDelayed = true,
        )

        return buildList {
            add(sellCurrencyDeepLink)
        }
    }

    private suspend fun onSellCurrencyDeepLink(userWallet: UserWallet, data: SellCurrencyDeepLink.Data) {
        val cryptoCurrency = getCryptoCurrencyUseCase(userWallet, data.currencyId).getOrNull()

        if (cryptoCurrency == null) {
            Timber.e("onSellCurrencyDeepLink cryptoCurrency is null")
            return
        }

        val route = AppRoute.Send(
            currency = cryptoCurrency,
            userWalletId = userWallet.walletId,
            transactionId = data.transactionId,
            destinationAddress = data.depositWalletAddress,
            amount = data.baseCurrencyAmount,
            tag = data.depositWalletAddressTag,
        )

        router.push(route)
    }
}