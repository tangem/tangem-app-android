package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

/**
 * Resolves the APY of a token as the user sees it: the market APY raised by an active Merkl campaign, or the plain
 * market APY when no campaign is running.
 *
 * @return null when the token has no yield market on [chainId], and when the markets could not be fetched at all
 */
class ResolveYieldApyUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(chainId: Int, tokenAddress: String): BigDecimal? {
        return getMarkets()
            .firstOrNull { it.chainId == chainId && it.tokenAddress.equals(tokenAddress, ignoreCase = true) }
            ?.effectiveApy
    }

    private suspend fun getMarkets(): List<YieldMarketToken> {
        val cached = yieldSupplyRepository.getCachedMarkets()
        if (!cached.isNullOrEmpty()) return cached

        // An APY is decoration: a failed fetch leaves the surface without a value instead of failing the screen.
        return runSuspendCatching { yieldSupplyRepository.updateMarkets() }
            .getOrElse { error ->
                TangemLogger.e("Unable to fetch yield markets to resolve an APY", error)
                emptyList()
            }
    }
}