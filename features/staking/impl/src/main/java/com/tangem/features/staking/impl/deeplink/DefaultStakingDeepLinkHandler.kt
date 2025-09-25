package com.tangem.features.staking.impl.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.GetYieldUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.staking.api.deeplink.StakingDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
internal class DefaultStakingDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val getYieldUseCase: GetYieldUseCase,
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase,
) : StakingDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val networkId = queryParams[NETWORK_ID_KEY]
        val tokenId = queryParams[TOKEN_ID_KEY]

        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        val selectedUserWalletId = getSelectedWalletSyncUseCase().getOrNull()?.walletId
        val walletId = queryParams[WALLET_ID_KEY]?.let(::UserWalletId) ?: selectedUserWalletId

        // If selected user wallet is different than from deeplink - ignore deeplink
        // If selected user wallet is null - ignore deeplink
        if (walletId != selectedUserWalletId || selectedUserWalletId == null) {
            Timber.e("Error on getting user wallet")
            return
        }

        scope.launch {
            val cryptoCurrency = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(selectedUserWalletId),
            )
                .orEmpty()
                .firstOrNull {
                    val isNetwork = it.network.backendId.equals(networkId, ignoreCase = true)
                    val isCurrency = it.id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true
                    isNetwork && isCurrency
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

            val isStakingEnabled = getStakingAvailabilityUseCase.invokeSync(
                userWalletId = selectedUserWalletId,
                cryptoCurrency = cryptoCurrency,
            ).getOrNull()

            if (isStakingEnabled !is StakingAvailability.Available) {
                return@launch
            }

            val yield = getYieldUseCase.invoke(
                cryptoCurrencyId = cryptoCurrency.id,
                symbol = cryptoCurrency.symbol,
            ).getOrElse {
                Timber.e("Staking is unavailable for ${cryptoCurrency.name}")
                return@launch
            }

            appRouter.push(
                AppRoute.Staking(
                    userWalletId = selectedUserWalletId,
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
}