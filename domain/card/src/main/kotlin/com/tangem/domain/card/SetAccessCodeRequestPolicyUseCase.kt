package com.tangem.domain.card

import com.tangem.domain.card.repository.CardSdkConfigRepository

/**
 * Set access code request policy by 'isBiometricsRequestPolicy'
 *
 * @property cardSdkConfigRepository repository for managing of CardSDK config
 *
[REDACTED_AUTHOR]
 */
class SetAccessCodeRequestPolicyUseCase(private val cardSdkConfigRepository: CardSdkConfigRepository) {

    operator fun invoke(isBiometricsRequestPolicy: Boolean) {
        cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy)
    }
}