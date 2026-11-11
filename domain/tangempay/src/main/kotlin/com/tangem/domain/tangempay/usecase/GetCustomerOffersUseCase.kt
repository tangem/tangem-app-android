package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CardIssueOffers
import com.tangem.domain.tangempay.model.CustomerOffers
import com.tangem.domain.tangempay.model.Offer
import com.tangem.domain.tangempay.model.deeplinkOffer
import com.tangem.domain.tangempay.model.plasticOffer
import com.tangem.domain.tangempay.model.plasticReissueOffer
import com.tangem.domain.tangempay.model.virtualOffer
import com.tangem.domain.tangempay.repository.CustomerOffersRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

/**
 * Loads customer offers from `GET /v1/customer/offers`, or, for a single product instance, from
 * `GET /v1/product-instances/{id}/offers`.
 *
 * Used by the issue-additional-card flow to gate the "+" action and to drive the cost popup.
 */
class GetCustomerOffersUseCase(
    private val customerOffersRepository: CustomerOffersRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        deeplinkValue: String? = null,
    ): Either<TangemPayApiError, List<Offer>> {
        return customerOffersRepository
            .getOffers(userWalletId = userWalletId, deeplinkValue = deeplinkValue)
            .map(CustomerOffers::orderable)
    }

    suspend fun campaignOffer(userWalletId: UserWalletId, deeplinkValue: String): Either<TangemPayApiError, Offer?> {
        return customerOffersRepository
            .getOffers(userWalletId = userWalletId, deeplinkValue = deeplinkValue)
            .map { offers -> offers.orderable.deeplinkOffer() }
    }

    suspend fun cardIssueOffers(userWalletId: UserWalletId): Either<TangemPayApiError, CardIssueOffers> {
        return customerOffersRepository.getOffers(userWalletId).map { offers ->
            CardIssueOffers(
                virtual = offers.orderable.virtualOffer(),
                plastic = offers.orderable.plasticOffer(),
                plasticArtworkUrl = offers.artwork[Offer.Type.CARD_ISSUE_PLASTIC_RAIN],
            )
        }
    }

    suspend fun additionalCardOffer(userWalletId: UserWalletId): Either<TangemPayApiError, Offer?> {
        return cardIssueOffers(userWalletId).map { it.virtual }
    }

    suspend fun plasticReissueOffer(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<TangemPayApiError, Offer?> {
        return customerOffersRepository
            .getProductInstanceOffers(userWalletId = userWalletId, productInstanceId = productInstanceId)
            .map { offers -> offers.orderable.plasticReissueOffer() }
    }
}