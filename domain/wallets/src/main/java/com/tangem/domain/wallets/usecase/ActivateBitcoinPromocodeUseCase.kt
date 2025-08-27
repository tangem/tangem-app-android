package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.repository.WalletsRepository
import javax.inject.Inject

class ActivateBitcoinPromocodeUseCase @Inject constructor(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(address: String, promoCode: String): Either<ActivatePromoCodeError, String> =
        walletsRepository.activatePromoCode(promoCode = promoCode, bitcoinAddress = address)
}