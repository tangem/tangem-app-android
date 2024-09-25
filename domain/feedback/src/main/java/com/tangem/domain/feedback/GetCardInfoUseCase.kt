package com.tangem.domain.feedback

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.domain.feedback.models.CardInfo
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

    operator fun invoke(scanResponse: ScanResponse): Either<Throwable, CardInfo> = catch {
        feedbackRepository.getCardInfo(scanResponse)
    }
}
