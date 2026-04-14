package com.tangem.data.dynamicaddresses

import com.tangem.crypto.hdWallet.DerivationPath
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
import com.tangem.blockchain.extensions.SimpleResult
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
                    token?.dynamicAddressesEnabled != true -> DynamicAddressesStatus.DISABLED
                    !isXpubAvailable(userWalletId, network) -> DynamicAddressesStatus.ENABLED_REQUIRES_SETUP
                    else -> DynamicAddressesStatus.ENABLED
                }
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun enable(userWalletId: UserWalletId, network: Network, xpub: String) {
        withContext(dispatchers.io) {
            val result = walletManagersFacade.enableXpubMode(userWalletId, network, xpub)
            if (result is SimpleResult.Failure) {
                error("Failed to enable xpub mode for $userWalletId / ${network.id}: ${result.error}")
            }
            updateTokenDynamicAddressesFlag(userWalletId, network, enabled = true)
            runSuspendCatching { accountsCRUDRepository.syncTokens(userWalletId) }
                .onFailure { throwable ->
                    TangemLogger.e(
                        messageString = "Failed to sync tokens after dynamic addresses enable for $userWalletId",
                        throwable = throwable,
                    )
                }
        }
    }

    override suspend fun disable(userWalletId: UserWalletId, network: Network) {
        withContext(dispatchers.io) {
            val result = walletManagersFacade.disableXpubMode(userWalletId, network)
            if (result is SimpleResult.Failure) {
                error("Failed to disable xpub mode for $userWalletId / ${network.id}: ${result.error}")
            }
            updateTokenDynamicAddressesFlag(userWalletId, network, enabled = false)
            runSuspendCatching { accountsCRUDRepository.syncTokens(userWalletId) }
                .onFailure { throwable ->
                    TangemLogger.e(
                        messageString = "Failed to sync tokens after dynamic addresses disable for $userWalletId",
                        throwable = throwable,
                    )
                }
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

    override suspend fun hasConflictingCustomTokens(userWalletId: UserWalletId, network: Network): Boolean {
        return withContext(dispatchers.io) {
            val response = walletAccountsFetcher.getSaved(userWalletId) ?: return@withContext false
            val baseDerivationPath = network.derivationPath.value ?: return@withContext false

            response.accounts
                .flatMap { it.tokens.orEmpty() }
                .any { token ->
                    val tokenDerivationPath = token.derivationPath ?: return@any false
                    token.networkId == network.rawId &&
                        tokenDerivationPath != baseDerivationPath &&
                        hasNonZeroChangeOrIndex(tokenDerivationPath, baseDerivationPath)
                }
        }
    }

    /**
     * Checks if the token's derivation path has the same first 3 nodes (purpose/coin/account)
     * as the base path but different change/index nodes (not both 0).
     */
    private fun hasNonZeroChangeOrIndex(tokenPath: String, basePath: String): Boolean {
        val tokenNodes = runCatching { DerivationPath(tokenPath).nodes }.getOrNull() ?: return false
        val baseNodes = runCatching { DerivationPath(basePath).nodes }.getOrNull() ?: return false

        if (tokenNodes.size < DERIVATION_NODE_COUNT || baseNodes.size < DERIVATION_NODE_COUNT) return false

        // First 3 nodes must match (purpose/coin/account) by value, ignoring hardening
        val isSameAccount = (0 until ACCOUNT_NODE_COUNT).all { i ->
            tokenNodes[i].getIndex(includeHardened = false) == baseNodes[i].getIndex(includeHardened = false)
        }
        if (!isSameAccount) return false

        // Check if change or index ≠ 0
        val change = tokenNodes[CHANGE_NODE_INDEX].getIndex(includeHardened = false)
        val index = tokenNodes[INDEX_NODE_INDEX].getIndex(includeHardened = false)

        return change != 0L || index != 0L
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

    private suspend fun isXpubAvailable(userWalletId: UserWalletId, network: Network): Boolean {
        // Check if WalletManager is already in XPUB mode (dynamic addresses was previously enabled on this device)
        return walletManagersFacade.getDynamicAddressesReceiveAddress(userWalletId, network) != null
    }

    private fun GetWalletAccountsResponse.findToken(network: Network): UserTokensResponse.Token? {
        return accounts
            .flatMap { it.tokens.orEmpty() }
            .find { it.matchesNetwork(network) }
    }

    private fun UserTokensResponse.Token.matchesNetwork(network: Network): Boolean {
        return networkId == network.rawId &&
            derivationPath == network.derivationPath.value &&
            contractAddress == null
    }

    private companion object {
        const val DERIVATION_NODE_COUNT = 5
        const val ACCOUNT_NODE_COUNT = 3
        const val CHANGE_NODE_INDEX = 3
        const val INDEX_NODE_INDEX = 4
    }
}