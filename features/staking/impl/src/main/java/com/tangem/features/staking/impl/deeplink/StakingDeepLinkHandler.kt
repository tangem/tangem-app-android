package com.tangem.features.staking.impl.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class StakingDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val queryParams = request.queryParams
        val networkId = queryParams[NETWORK_ID_KEY]
        val tokenId = queryParams[TOKEN_ID_KEY]

        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        val selectedUserWalletId = getSelectedWalletSyncUseCase().getOrNull()?.walletId
        val rawWalletId = queryParams[WALLET_ID_KEY]
        val walletId = if (rawWalletId != null) UserWalletId.fromHexOrNull(rawWalletId) else selectedUserWalletId

        // If selected user wallet is different than from deeplink - ignore deeplink
        // If selected user wallet is null - ignore deeplink
        if (walletId != selectedUserWalletId || selectedUserWalletId == null) {
            TangemLogger.e("Error on getting user wallet")
            return
        }

        request.scope.launch {
            val cryptoCurrency = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(selectedUserWalletId),
            )
                .orEmpty()
                .firstOrNull { currency ->
                    val isNetwork = currency.network.rawId.equals(networkId, ignoreCase = true)
                    val isCurrency = currency.id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true
                    isNetwork && isCurrency
                }

            if (cryptoCurrency == null) {
                TangemLogger.e(
                    """
                        Could not get crypto currency for
                        |- $NETWORK_ID_KEY: ${networkId.orEmpty()}
                        |- $TOKEN_ID_KEY: ${tokenId.orEmpty()}
                    """.trimIndent(),
                )
                return@launch
            }

            val availability: StakingAvailability? = getStakingAvailabilityUseCase.invokeSync(
                userWalletId = selectedUserWalletId,
                cryptoCurrency = cryptoCurrency,
            ).getOrNull()

            val option = availability?.optionOrNull
            if (option == null) {
                TangemLogger.e("Staking is unavailable for ${cryptoCurrency.name}")
                return@launch
            }

            appRouter.push(
                AppRoute.Staking(
                    userWalletId = selectedUserWalletId,
                    cryptoCurrency = cryptoCurrency,
                    integrationId = option.integrationId,
                ),
            )
        }
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://staking"
    }
}