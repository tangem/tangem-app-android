package com.tangem.domain.yield.supply.promo.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.YieldPromoRepository
import com.tangem.lib.crypto.BlockchainUtils

/**
 * Returns `true` iff the given token is in the active promo list AND the user has not started boost yet.
 *
 * Short-circuits to `false` on:
 *   - non-Token currency
 *   - promo `None` (no active promo)
 *   - status not `NotStarted` (already Active / Completed / Disqualified)
 *
 * Any underlying repository failure surfaces as `Either.Left`.
 *
 * Feature-toggle and redesign-flag gating is the caller's responsibility — keep this use case
 * decoupled from feature-layer toggles to avoid the cyclic dependency `domain -> features`.
 */
class IsYieldBoostPromoEnabledForTokenUseCase(
    private val repository: YieldPromoRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<Throwable, Boolean> = Either.catch {
        val token = cryptoCurrency as? CryptoCurrency.Token ?: return@catch false

        val promo = repository.getYieldBoostPromo(userWalletId)
        if (promo !is YieldBoostPromo.Active) return@catch false

        val shouldIgnoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId)
        val isTokenMatched = promo.tokens.any { promoToken ->
            promoToken.contractAddress.equals(token.contractAddress, ignoreCase = shouldIgnoreCase) &&
                promoToken.networkId == token.network.rawId
        }
        if (!isTokenMatched) return@catch false

        val status = repository.getYieldBoostStatus(userWalletId)
        status is YieldBoostStatus.NotStarted
    }
}