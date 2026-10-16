package com.tangem.features.yield.supply.impl.promobottomsheet

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnYieldOffers
import com.tangem.features.yield.supply.api.promobottomsheet.ResolveYieldModePromoUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoOffer
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoResolution
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultResolveYieldModePromoUseCase @Inject constructor() : ResolveYieldModePromoUseCase {

    override fun invoke(
        statuses: List<CryptoCurrencyStatus>,
        yieldApys: Map<String, BigDecimal>,
        earnOffers: EarnYieldOffers,
    ): YieldModePromoResolution {
        if (earnOffers.total <= 0) return YieldModePromoResolution.NoEligibleToken
        if (statuses.any { it.value.yieldSupplyStatus?.isActive == true }) {
            return YieldModePromoResolution.NoEligibleToken
        }

        // A token held in several accounts is one position: its statuses differ only by derivation
        val positions = statuses
            .filter { status -> status.currency is CryptoCurrency.Token && !status.currency.isJoint }
            .groupBy { status -> (status.currency as CryptoCurrency.Token).yieldSupplyKey() }
            .filterKeys { key -> key in yieldApys }

        if (positions.isEmpty()) return YieldModePromoResolution.NoEligibleToken
        if (positions.values.flatten().any { it.isLoading() }) return YieldModePromoResolution.Waiting

        val candidate = positions
            .mapNotNull { (key, accountStatuses) -> candidateOrNull(key, accountStatuses, yieldApys.getValue(key)) }
            .minWithOrNull(BEST_FIRST)
            ?: return YieldModePromoResolution.NoEligibleToken

        val globalMaxApy = earnOffers.best?.toOffer(earnOffers.total)
            ?: return YieldModePromoResolution.DataNotReady

        return YieldModePromoResolution.Ready(
            userMaxApy = candidate.toOffer(earnOffers.total),
            globalMaxApy = globalMaxApy,
        )
    }

    private fun candidateOrNull(key: String, accountStatuses: List<CryptoCurrencyStatus>, apy: BigDecimal): Candidate? {
        if (apy.signum() <= 0) return null
        if (accountStatuses.any { it.isFailed() }) return null

        val amount = accountStatuses.sumOf { it.value.amount.orZero() }
        if (amount.signum() <= 0) return null

        val token = accountStatuses.first().currency as CryptoCurrency.Token
        return Candidate(
            key = key,
            token = token,
            apy = apy,
            fiatAmount = accountStatuses.sumOf { it.value.fiatAmount.orZero() },
            chainId = Blockchain.fromNetworkId(token.network.rawId)?.getChainId(),
        )
    }

    // Cached values are replaced on the first refresh of the launch, so they don't count as received yet
    private fun CryptoCurrencyStatus.isLoading(): Boolean {
        return value is CryptoCurrencyStatus.Loading || value.sources.networkSource == StatusSource.CACHE
    }

    // A refresh that failed over a cached value keeps it as ONLY_CACHE instead of an error state
    private fun CryptoCurrencyStatus.isFailed(): Boolean {
        return value.isError || value.sources.networkSource == StatusSource.ONLY_CACHE
    }

    private fun EarnToken.toOffer(tokensCount: Int): YieldModePromoOffer? {
        val apyPercent = apy.toBigDecimalOrNull()?.movePointRight(2)?.takeIf { it.signum() > 0 } ?: return null

        return YieldModePromoOffer(
            apy = apyPercent,
            tokensCount = tokensCount,
            tokenSymbol = tokenSymbol,
            blockchain = Blockchain.fromNetworkId(networkId)?.fullName ?: networkId,
        )
    }

    private fun Candidate.toOffer(tokensCount: Int) = YieldModePromoOffer(
        apy = apy,
        tokensCount = tokensCount,
        tokenSymbol = token.symbol,
        blockchain = token.network.name,
    )

    private data class Candidate(
        val key: String,
        val token: CryptoCurrency.Token,
        val apy: BigDecimal,
        val fiatAmount: BigDecimal,
        val chainId: Int?,
    )

    private companion object {
        // The spec's tie-break, with the numeric chain id the iOS app compares as well
        val BEST_FIRST: Comparator<Candidate> = compareByDescending<Candidate> { it.apy }
            .thenByDescending { it.fiatAmount }
            .thenBy(nullsLast()) { it.chainId }
            .thenBy { it.key }
    }
}