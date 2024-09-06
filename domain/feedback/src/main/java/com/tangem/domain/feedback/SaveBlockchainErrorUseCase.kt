package com.tangem.domain.feedback

import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.repository.FeedbackRepository

/**
 * Save last blockchain error
 *
 * @property feedbackRepository feedback repository
 *
 * @author Andrew Khokhlov on 20/05/2024
 */
class SaveBlockchainErrorUseCase(
    private val feedbackRepository: FeedbackRepository,
) {

    operator fun invoke(error: BlockchainErrorInfo) {
        feedbackRepository.saveBlockchainErrorInfo(error = error)
    }
}
