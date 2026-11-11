package com.tangem.domain.yield.supply.status

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.TokensFeatureToggles

/**
 * Refreshes the Yield Module statuses of the given tokens as part of a balance update. Does nothing unless
 * [TokensFeatureToggles.isYieldContributionEnabled], since nothing reads the statuses until then.
 */
class YieldModuleStatusContributionFetcher(
    private val yieldModuleStatusFetcher: YieldModuleStatusFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) : BalanceContributionFetcher {

    override suspend fun fetch(
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        if (!tokensFeatureToggles.isYieldContributionEnabled) return Unit.right()

        // A joint account is a Safe contract and owns no Yield Module
        val tokens = currencies
            .filterIsInstance<CryptoCurrency.Token>()
            .filterNotTo(hashSetOf()) { it.isJoint }

        if (tokens.isEmpty()) return Unit.right()

        return yieldModuleStatusFetcher(
            params = YieldModuleStatusFetcher.Params(userWalletId = userWalletId, tokens = tokens),
        )
    }
}