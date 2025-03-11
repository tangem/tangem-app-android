package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.AppRatingRepository
import kotlinx.coroutines.flow.Flow

class IsReadyToShowRateAppUseCase(private val appRatingRepository: AppRatingRepository) {

    operator fun invoke(): Flow<Boolean> = appRatingRepository.isReadyToShow()
}