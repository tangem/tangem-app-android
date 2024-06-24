package com.tangem.domain.feedback

import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.models.scan.ScanResponse

/**
 * UseCase for creating 'CardInfo'
 *
 * @property feedbackRepository feedback repository
 *
 * @author Andrew Khokhlov on 19/06/2024
 */
class GetCardInfoUseCase(
    private val feedbackRepository: FeedbackRepository,
) {

    suspend operator fun invoke(scanResponse: ScanResponse) = feedbackRepository.getCardInfo(scanResponse)
}
