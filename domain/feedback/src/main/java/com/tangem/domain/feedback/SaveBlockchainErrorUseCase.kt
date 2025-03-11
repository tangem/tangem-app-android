package com.tangem.domain.feedback

import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.repository.FeedbackRepository

/**
 * Save last blockchain error
 *
 * @property feedbackRepository feedback repository
 *
[REDACTED_AUTHOR]
 */
class SaveBlockchainErrorUseCase(
    private val feedbackRepository: FeedbackRepository,
) {

    operator fun invoke(error: BlockchainErrorInfo) {
        feedbackRepository.saveBlockchainErrorInfo(error = error)
    }
}