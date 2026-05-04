package com.tangem.data.dynamicaddresses

import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultDynamicAddressesRepository(
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val walletAccountsSaver: WalletAccountsSaver,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : DynamicAddressesRepository {

    override fun getStatus(userWalletId: UserWalletId, network: Network): Flow<DynamicAddressesStatus> {
        return walletAccountsFetcher.get(userWalletId)
            .map { response ->
                val token = response.findToken(network)
                when {
                    token?.dynamicAddressesEnabled == true -> DynamicAddressesStatus.ENABLED
                    else -> DynamicAddressesStatus.DISABLED
                }
                // TODO handle ENABLED_REQUIRES_SETUP when XPUB is not derived locally
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun enable(userWalletId: UserWalletId, network: Network, xpub: String) {
        withContext(dispatchers.io) {
            walletManagersFacade.enableXpubMode(userWalletId, network, xpub)
            updateTokenDynamicAddressesFlag(userWalletId, network, enabled = true)
            runSuspendCatching { accountsCRUDRepository.syncTokens(userWalletId) }
                .onFailure { TangemLogger.e("Failed to sync tokens after DA enable for $userWalletId", it) }
        }
    }

    override suspend fun disable(userWalletId: UserWalletId, network: Network) {
        withContext(dispatchers.io) {
            walletManagersFacade.disableXpubMode(userWalletId, network)
            updateTokenDynamicAddressesFlag(userWalletId, network, enabled = false)
            runSuspendCatching { accountsCRUDRepository.syncTokens(userWalletId) }
                .onFailure { TangemLogger.e("Failed to sync tokens after DA disable for $userWalletId", it) }
        }
    }

    override suspend fun getReceiveAddress(userWalletId: UserWalletId, network: Network): String {
        return walletManagersFacade.getDynamicAddressesReceiveAddress(userWalletId, network)
            ?: error("Dynamic receive address not available for $userWalletId / ${network.id}")
    }

    override suspend fun getLastUsedReceiveAddress(userWalletId: UserWalletId, network: Network): String? {
        return walletManagersFacade.getDynamicAddressesLastUsedReceiveAddress(userWalletId, network)
    }

    override suspend fun hasNonBaseBalances(userWalletId: UserWalletId, network: Network): Boolean {
        return walletManagersFacade.hasDynamicAddressesNonBaseBalances(userWalletId, network)
    }

    private suspend fun updateTokenDynamicAddressesFlag(
        userWalletId: UserWalletId,
        network: Network,
        enabled: Boolean,
    ) {
        walletAccountsSaver.update(userWalletId) { response ->
            response?.copy(
                accounts = response.accounts.map { account ->
                    account.copy(
                        tokens = account.tokens?.map { token ->
                            if (token.matchesNetwork(network)) {
                                token.copy(dynamicAddressesEnabled = enabled)
                            } else {
                                token
                            }
                        },
                    )
                },
            )
        }
    }

    private fun GetWalletAccountsResponse.findToken(network: Network): UserTokensResponse.Token? {
        return accounts
            .flatMap { it.tokens.orEmpty() }
            .find { it.matchesNetwork(network) }
    }

    private fun UserTokensResponse.Token.matchesNetwork(network: Network): Boolean {
        return networkId == network.backendId &&
            derivationPath == network.derivationPath.value &&
            contractAddress == null
    }
}