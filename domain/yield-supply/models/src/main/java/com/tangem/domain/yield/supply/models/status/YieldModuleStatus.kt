package com.tangem.domain.yield.supply.models.status

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.normalizeEvmAddress
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import java.math.BigDecimal

/**
 * Status of a single token in the user's Yield Module on [networkId].
 *
 * Keyed by the token contract rather than by a portfolio currency: an entry may belong to a token that is not in the
 * portfolio (e.g. WETH supplied on behalf of the network coin).
 *
 * @property tokenAddress             token contract address, normalized with [normalizeEvmAddress]
 * @property effectiveProtocolBalance supplied balance excluding the service fee; read only for an active token
 * @property effectiveBalance         wallet + supplied balance, read alongside [effectiveProtocolBalance] in one SDK
 * call; read only for an active token, `null` in positions stored before it existed
 */
data class YieldModuleStatus(
    val networkId: Network.ID,
    val tokenAddress: String,
    val isActive: Boolean,
    val isInitialized: Boolean,
    val isAllowedToSpend: Boolean,
    val effectiveProtocolBalance: BigDecimal?,
    val effectiveBalance: BigDecimal?,
    val source: StatusSource,
) {

    val key: Key get() = Key(networkId = networkId, tokenAddress = tokenAddress)

    data class Key(val networkId: Network.ID, val tokenAddress: String) {

        constructor(token: CryptoCurrency.Token) : this(
            networkId = token.network.id,
            tokenAddress = token.contractAddress.normalizeEvmAddress(),
        )
    }
}

fun YieldModuleStatus.toYieldSupplyStatus(): YieldSupplyStatus = YieldSupplyStatus(
    isActive = isActive,
    isInitialized = isInitialized,
    isAllowedToSpend = isAllowedToSpend,
    effectiveProtocolBalance = effectiveProtocolBalance,
)

fun Collection<YieldModuleStatus>.findFor(token: CryptoCurrency.Token): YieldModuleStatus? {
    val key = YieldModuleStatus.Key(token)
    return firstOrNull { it.key == key }
}