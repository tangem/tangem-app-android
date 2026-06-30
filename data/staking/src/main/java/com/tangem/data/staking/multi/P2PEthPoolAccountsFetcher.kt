package com.tangem.data.staking.multi

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolAccountsListRequest
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountsListResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolResponse
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Fetches P2P ETH Pool account responses via the batch strategy:
 * one POST per vault sending all delegator addresses at once.
 *
 * @property p2pEthPoolApi P2PEthPool API
 * @property dispatchers   coroutine dispatcher provider
 *
[REDACTED_AUTHOR]
 */
internal class P2PEthPoolAccountsFetcher(
    private val p2pEthPoolApi: P2PEthPoolApi,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun fetchBatch(vaults: List<P2PEthPoolVault>, addresses: Set<String>): Set<P2PEthPoolAccountResponse> =
        coroutineScope {
            val request = P2PEthPoolAccountsListRequest(delegatorAddresses = addresses.toList())

            vaults
                .map { vault ->
                    async(dispatchers.io) {
                        runSuspendCatching {
                            val response = p2pEthPoolApi.getAccountsList(
                                network = P2PEthPoolStakingConfig.activeNetwork.value,
                                vaultAddress = vault.vaultAddress,
                                body = request,
                            )

                            mapBatchVaultResponse(vault = vault, response = response)
                        }.getOrElse { error ->
                            TangemLogger.w(
                                "Failed to fetch P2PEthPool batch balances for vault ${vault.vaultAddress}",
                                error,
                            )
                            emptyList()
                        }
                    }
                }
                .awaitAll()
                .flatten()
                .toSet()
        }

    private fun mapBatchVaultResponse(
        vault: P2PEthPoolVault,
        response: ApiResponse<P2PEthPoolResponse<P2PEthPoolAccountsListResponse>>,
    ): List<P2PEthPoolAccountResponse> {
        return when (response) {
            is ApiResponse.Success -> {
                val data = response.data
                if (data.error != null) {
                    TangemLogger.w(
                        "P2PEthPool batch API returned error for vault " +
                            "${vault.vaultAddress}: ${data.error}",
                    )
                    emptyList()
                } else {
                    val result = requireNotNull(data.result) {
                        "Result is null in successful response"
                    }
                    result.list.mapNotNull { item ->
                        if (item.error != null) {
                            TangemLogger.w(
                                "P2PEthPool batch item error for vault " +
                                    "${vault.vaultAddress}, address " +
                                    "${item.delegatorAddress}: ${item.error}",
                            )
                            null
                        } else {
                            item.account
                        }
                    }
                }
            }
            is ApiResponse.Error -> {
                TangemLogger.w(
                    "Failed to fetch P2PEthPool batch balances for vault " +
                        "${vault.vaultAddress}",
                    response.cause,
                )
                emptyList()
            }
        }
    }
}