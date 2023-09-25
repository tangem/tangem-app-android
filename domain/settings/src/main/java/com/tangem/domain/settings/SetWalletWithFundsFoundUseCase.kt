package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.AppRatingRepository

class SetWalletWithFundsFoundUseCase(private val appRatingRepository: AppRatingRepository) {

    suspend operator fun invoke() = appRatingRepository.setWalletWithFundsFound()
}