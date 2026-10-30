package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

/** One availability check; `null` on the right means "not yet", the caller decides when to ask again. */
class GetCollectibleBuybackOfferUseCase @Inject constructor(private val repository: CollectiblesRepository) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        nftAddress: String,
    ): Either<CollectiblesError, CollectibleBuybackOffer?> {
        return runSuspendCatching { repository.getBuybackOffer(userWalletId = userWalletId, nftAddress = nftAddress) }
            .fold(
                onSuccess = { it.right() },
                onFailure = { CollectiblesError.Api(it).left() },
            )
    }
}