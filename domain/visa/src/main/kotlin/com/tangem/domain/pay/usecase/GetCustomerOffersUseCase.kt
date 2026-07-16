package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.visa.error.VisaApiError

/**
 * Loads customer offers from `GET /v1/customer/offers`.
 *
 * Used by the issue-additional-card flow to gate the "+" action and to drive the cost popup.
 */
class GetCustomerOffersUseCase(
    private val customerOffersRepository: CustomerOffersRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, List<Offer>> {
        return customerOffersRepository.getOffers(userWalletId)
    }

    suspend fun additionalCardOffer(userWalletId: UserWalletId): Either<VisaApiError, Offer?> {
        return customerOffersRepository.getOffers(userWalletId).map { offers ->
            offers.firstOrNull { it.type == Offer.Type.CARD_ISSUE_VIRTUAL_RAIN }
        }
    }
}