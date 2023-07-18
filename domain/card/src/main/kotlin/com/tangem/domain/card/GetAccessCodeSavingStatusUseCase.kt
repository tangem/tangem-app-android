package com.tangem.domain.card

import com.tangem.domain.card.repository.CardSdkConfigRepository

/**
 * Use case for getting access code saving status
 *
 * @property cardSdkConfigRepository repository for managing of CardSDK config
 *
[REDACTED_AUTHOR]
 */
class GetAccessCodeSavingStatusUseCase(private val cardSdkConfigRepository: CardSdkConfigRepository) {

    operator fun invoke(): Boolean = cardSdkConfigRepository.isAccessCodeSavingEnabled()
}