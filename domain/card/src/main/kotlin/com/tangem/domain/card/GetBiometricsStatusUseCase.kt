package com.tangem.domain.card

import com.tangem.domain.card.repository.CardSdkConfigRepository

/**
 * Check if config has biometrics request policy
 *
 * @property cardSdkConfigRepository repository for managing of CardSDK config
 *
[REDACTED_AUTHOR]
 */
class GetBiometricsStatusUseCase(private val cardSdkConfigRepository: CardSdkConfigRepository) {

    operator fun invoke(): Boolean = cardSdkConfigRepository.isBiometricsRequestPolicy()
}