package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserTokensResponseAddressesEnricher @Inject constructor(
    private val walletsRepository: WalletsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, response: UserTokensResponse): UserTokensResponse {
        val isNotificationsEnabled = walletsRepository.isNotificationsEnabled(userWalletId)

        return withContext(dispatchers.default) {
            val addressByToken = if (isNotificationsEnabled) {
                response.tokens.associateWith { token ->
                    val blockchain = Blockchain.fromNetworkId(token.networkId) ?: return@associateWith null

                    val walletManager = walletManagersFacade.getOrCreateWalletManager(
                        userWalletId = userWalletId,
                        blockchain = blockchain,
                        derivationPath = token.derivationPath,
                    )

                    walletManager?.wallet?.addresses?.map(Address::value)
                }
            } else {
                emptyMap()
            }

            val enrichedTokens = response.tokens.map { token ->
                if (isNotificationsEnabled) {
                    val addresses = addressByToken[token] ?: return@map token

                    token.copy(addresses = addresses)
                } else {
                    token.copy(addresses = emptyList())
                }
            }

            response.copy(tokens = enrichedTokens, notifyStatus = isNotificationsEnabled)
        }
    }
}