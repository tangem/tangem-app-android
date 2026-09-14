package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.jointaccount.derivation.isJointAccountReservedPath
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Lazy
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserTokensResponseAddressesEnricher @Inject constructor(
    private val walletsRepository: WalletsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
    private val jointAccountSafeAddressResolver: Lazy<JointAccountSafeAddressResolver>,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, response: UserTokensResponse): UserTokensResponse {
        val isNotificationsEnabled = walletsRepository.isNotificationsEnabled(userWalletId)

        return withContext(dispatchers.default) {
            val addressByToken = if (isNotificationsEnabled) {
                val safeAddressByOwnerPath = jointSafeAddresses(userWalletId, response.tokens)

                response.tokens.associateWith { token ->
                    val ownerPath = token.derivationPath
                    if (ownerPath != null && isJointAccountReservedPath(rawPath = ownerPath)) {
                        return@associateWith safeAddressByOwnerPath[ownerPath]?.let(::listOf)
                    }

                    val blockchain = Blockchain.fromNetworkId(token.networkId) ?: return@associateWith null

                    val walletManager = walletManagersFacade.getOrCreateWalletManager(
                        userWalletId = userWalletId,
                        blockchain = blockchain,
                        derivationPath = ownerPath,
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

    /**
     * A joint token must be registered on the Safe address: the wallet's own keys hold nothing on that network, so
     * a notification for the owner-key address would be about an address the user never receives to. The resolver
     * waits for the accounts data, so it is asked only about the joint paths this response actually carries.
     */
    private suspend fun jointSafeAddresses(
        userWalletId: UserWalletId,
        tokens: List<UserTokensResponse.Token>,
    ): Map<String, String> {
        val jointPaths = tokens
            .mapNotNull { token -> token.derivationPath?.takeIf { isJointAccountReservedPath(rawPath = it) } }
            .toSet()

        if (jointPaths.isEmpty()) return emptyMap()

        return jointAccountSafeAddressResolver.get().resolveByOwnerPath(userWalletId, jointPaths)
    }
}