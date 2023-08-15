package com.tangem.tap.features.customtoken.impl.data

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.tap.features.customtoken.impl.data.converters.FoundTokenConverter
import com.tangem.tap.features.customtoken.impl.domain.CustomTokenRepository
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Default implementation of custom token repository
 *
 * @property tangemTechApi    TangemTech API
 * @property dispatchers      coroutine dispatchers provider
 * @property reduxStateHolder redux state holder
 *
[REDACTED_AUTHOR]
 */
class DefaultCustomTokenRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: AppStateHolder,
) : CustomTokenRepository {

    override suspend fun findToken(address: String, networkId: String?): FoundToken {
        val scanResponse = requireNotNull(reduxStateHolder.scanResponse)
        val supportedTokenNetworkIds = requireNotNull(scanResponse.card)
            .supportedBlockchains(scanResponse.cardTypesResolver)
            .filter(Blockchain::canHandleTokens)
            .map(Blockchain::toNetworkId)

        return withContext(dispatchers.io) {
            val foundCoin = tangemTechApi.getCoins(
                contractAddress = address,
                networkIds = networkId ?: supportedTokenNetworkIds.joinToString(separator = ","),
            )
                .coins.firstNotNullOfOrNull { coin ->
                    val networksWithTheSameAddress = coin.networks.filter { network ->
                        (network.contractAddress != null || network.decimalCount != null) &&
                            network.contractAddress?.equals(address, ignoreCase = true) == true &&
                            supportedTokenNetworkIds.contains(network.networkId)
                    }

                    if (networksWithTheSameAddress.isNotEmpty()) {
                        coin.copy(networks = networksWithTheSameAddress)
                    } else {
                        null
                    }
                }

            foundCoin?.let(FoundTokenConverter::convert) ?: error("Token not found")
        }
    }
}