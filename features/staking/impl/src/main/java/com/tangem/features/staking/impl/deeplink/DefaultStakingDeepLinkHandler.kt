package com.tangem.features.staking.impl.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.staking.GetYieldUseCase
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.staking.api.deeplink.StakingDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultStakingDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val getYieldUseCase: GetYieldUseCase,
) : StakingDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        val userWalletId = queryParams[WALLET_ID_KEY]?.let(::UserWalletId)
            ?: getSelectedWalletSyncUseCase().getOrNull()?.walletId

        if (userWalletId == null) {
            Timber.e("Error on getting user wallet")
            return
        }

        val networkId = queryParams[NETWORK_ID_KEY]
        val tokenId = queryParams[TOKEN_ID_KEY]

        scope.launch {
            val cryptoCurrency = getCryptoCurrenciesUseCase(userWalletId = userWalletId).getOrElse {
                Timber.e("Error on getting crypto currency list")
                return@launch
            }.firstOrNull {
                it.network.backendId == networkId && it.id.rawCurrencyId?.value == tokenId
            }

            if (cryptoCurrency == null) {
                Timber.e(
                    """
                        Could not get crypto currency for
                        |- $NETWORK_ID_KEY: $networkId
                        |- $TOKEN_ID_KEY: $tokenId
                    """.trimIndent(),
                )
                return@launch
            }

            val yield = getYieldUseCase.invoke(
                cryptoCurrencyId = cryptoCurrency.id,
                symbol = cryptoCurrency.symbol,
            ).getOrElse {
                error("Staking is unavailable for ${cryptoCurrency.name}")
                return@launch
            }

            appRouter.push(
                AppRoute.Staking(
                    userWalletId = userWalletId,
                    cryptoCurrencyId = cryptoCurrency.id,
                    yieldId = yield.id,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : StakingDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultStakingDeepLinkHandler
    }

    private companion object {
        const val WALLET_ID_KEY = "walletId"
        const val NETWORK_ID_KEY = "network_id"
        const val TOKEN_ID_KEY = "token_id"
    }
}