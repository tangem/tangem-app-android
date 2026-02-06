package com.tangem.feature.referral.domain

import arrow.core.Either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import javax.inject.Inject

class SetShouldShowMobileWalletPromoUseCase @Inject constructor(
    private val mobileWalletPromoRepository: MobileWalletPromoRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(): Either<Throwable, Unit> = Either.catch {
        val wallets = userWalletsListRepository.userWallets.value
        if (wallets.isNullOrEmpty()) {
            mobileWalletPromoRepository.setShouldShowMobileWalletPromo(true)
        }
    }
}