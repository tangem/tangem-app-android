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
* [REDACTED_AUTHOR]
 */
class GetCardInfoUseCase(
    private val feedbackRepository: FeedbackRepository,
) {

    suspend operator fun invoke(scanResponse: ScanResponse): Either<Throwable, CardInfo> {
        return catch { feedbackRepository.getCardInfo(scanResponse) }
    }
}
